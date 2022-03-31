package service

import zio._
import zio.stream._
import zio.json._
import zio.process._

import java.nio.charset.StandardCharsets
import java.nio.file.Path

import model._
import Model._

import repositories._


trait ShellProcess {

  def run(shell: ShellProcess.Shell, commands: Chunk[String]): UIO[ReplayFile]

}

object ShellProcess {

  sealed trait ProcessLog 
  object ProcessLog {
    final case class Command(command: String, nanoTime: Long) extends ProcessLog
    final case class Output(byte: Byte, nanoTime: Long) extends ProcessLog
  }

  final case class Shell(prompt: String, shell: Array[String])
  
  object Shell {

    val scala = Shell("scala", Array("scala", "-Dscala.color"))

  }

  def run(shell: Shell, commands: Chunk[String]) = ZIO.serviceWithZIO[ShellProcess](_.run(shell, commands))

  val live = (ShellProcessLive.apply _).toLayer
  
}

import ShellProcess._

case class ShellProcessLive(console: Console, clock: Clock) extends ShellProcess {
  
  private case class ShellProcessImpl(prompt: String, process: Process, queue: Queue[Chunk[Byte]], start: Long, processLogsRef: Ref[Vector[ProcessLog]]) {
    val promptAsByte = prompt.getBytes().toVector

  def receiveOutput(b: Byte): ZIO[Any, Throwable, Unit] = for {
    nanoTime <- clock.nanoTime
    output = ProcessLog.Output(b, nanoTime)
    _ <- processLogsRef.getAndUpdate(v => v.appended(output))
    _ <- console.print(new String(Array(b)))
  } yield () 

  def isPrompt = for {
    processLogs <- processLogsRef.get
    size = processLogs.size
    lasts = processLogs.slice(size - prompt.length(), size).collect {
      case ProcessLog.Output(b, _) => b
    }
  } yield lasts == promptAsByte

  def waitForPrompt: ZIO[Any, Throwable, Unit] = isPrompt flatMap { isPrompt =>
    if (isPrompt) {
      ZIO.unit
    } else {
      clock.sleep(100.millis).flatMap(_ => waitForPrompt)
    }
  }

  // inc() => do nothing
  // def inc() = {\ncounter = counter + 1\n} => printLine(command)
  // counter => printLine
  def fixDisplayCommand(command: String) = (command.contains("("), command.contains("\n")) match {
    case (true, false)  => None
    case (true, true)  => None
    case (false, _) => Some(command)
  }

  def typeCommand(command: String): ZIO[Any, Throwable, Unit] = for {
    _ <- waitForPrompt
    nanoTime <- clock.nanoTime
    input = ProcessLog.Command(command, nanoTime)
    _ <- processLogsRef.getAndUpdate(v => v.appended(input))
    _ <- queue.offer(toChunk(command + "\n"))
    _ <- fixDisplayCommand(command).map(console.printLine(_)).getOrElse(ZIO.unit)
    _ <- clock.sleep(50.millis)
  } yield ()

  def typeCommands(commands: Array[String]): UIO[Unit] = ZIO.foreach(commands)(typeCommand).as(()).orDie

  private def toChunk(s: String) = Chunk.fromArray(s.getBytes(StandardCharsets.ISO_8859_1))

  def cleanLogs(start: Long, logs: Vector[ProcessLog]): Vector[(Double, String)] = {
    type State = ((Double, String), Vector[(Double, String)])
    val init: State = ((0, ""), Vector())

    def reduce(state: State, op: (Double, String)): State = {
      val offsetLimit = 0.1

      val ((offsetBegin, s), v) = state
      val (offsetEnd, string) = op

      val diffTime = offsetEnd - offsetBegin
      val accumulate = (diffTime < offsetLimit) && !string.contains("\n")

      if (accumulate) {
        ((offsetBegin, s + string), v)
      } else {
        val newOp = (diffTime, s + string)
        ((offsetEnd, ""), v.appended(newOp))
      }
    }

    logs
    .collect(_ match {
      case ProcessLog.Command(command, nanoTime) if !command.contains("(") => ((nanoTime - start).toDouble / 1000000000.0, command + "\n")
      case ProcessLog.Output(byte, nanoTime) => ((nanoTime - start).toDouble / 1000000000.0, new String(Array(byte)))
    })
    .foldLeft(init)(reduce _)._2
  }

  def replayFile = for {
    logs <- processLogsRef.get
    header = """Script started on 2022-03-20 07:43:49+01:00 [TERM="xterm-256color" TTY="/dev/pts/6" COLUMNS="118" LINES="14"]""""
    footer = """Script done on 2022-03-20 07:44:11+01:00 [COMMAND_EXIT_CODE="0"]"""
    result = ReplayFile(header, footer, Chunk.fromArray(cleanLogs(start, logs).toArray))
  } yield result
  }

  def run(shell: ShellProcess.Shell, commands: Chunk[String]) = for {
    queue <- Queue.unbounded[Chunk[Byte]]
    process <- Command(shell.shell.head, shell.shell.tail:_*).stdin(ProcessInput.fromQueue(queue)).run.orDie
    start <- clock.nanoTime
    processLogsRef <- Ref.make[Vector[ProcessLog]](Vector())
    shellProcess = ShellProcessImpl(shell.prompt, process, queue, start, processLogsRef)
    _ <- process.stdout.stream.foreach(s => shellProcess.receiveOutput(s)).fork
    _ <- shellProcess.typeCommands(commands.toArray)
      replayFile <- shellProcess.replayFile
  } yield replayFile
  
  /*
  private case class Shell(cmd: Array[String], commands: Chunk[String]) {
    def run(): ZIO[Console & Clock, Throwable, ReplayFile] = for {
      shellProcess <- ShellProcess.make("scala> ", cmd)
      _ <- shellProcess.typeCommands(commands.toArray)
      replayFile <- shellProcess.replayFile
    } yield replayFile
  }
*/

  
    /*
  def make(prompt: String, command: Array[String]) = for {
    queue <- Queue.unbounded[Chunk[Byte]]
    process <- Command(command.head, command.tail:_*).stdin(ProcessInput.fromQueue(queue)).run
    start <- Clock.nanoTime
    processLogsRef <- Ref.make[Vector[ProcessLog]](Vector())
    shellProcess = ShellProcessImpl(prompt, process, queue, start, processLogsRef, console, clock)
    _ <- process.stdout.stream.foreach(s => shellProcess.receiveOutput(s)).fork
  } yield shellProcess
*/


}

