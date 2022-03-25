package service

import zio._
import zio.stream._
import zio.json._
import zio.process._

import java.nio.file.Path

import model._
import Model._

import repositories._
import java.nio.charset.StandardCharsets

trait Service {

  def convertFromJson(a: Path, b: Path, c: Path): ZIO[Any, Throwable, Unit]
  def convertToJson(a: Path, b: Path, c: Path): ZIO[Any, Throwable, Unit]
  def replay(replayFile: Path): ZIO[Any, Throwable, Unit]
  def extract(jsonFile: Path, commandsFile: Path): ZIO[Any, Throwable, Unit]
  def play(commandFile: Path, replayFile: Path): ZIO[Any, Throwable, Unit]

}

object Service {

  def convertFromJson(a: Path, b: Path, c: Path) = ZIO.serviceWithZIO[Service](_.convertFromJson(a, b, c))
  def convertToJson(a: Path, b: Path, c: Path) = ZIO.serviceWithZIO[Service](_.convertToJson(a, b, c))
  def replay(replayFile: Path) = ZIO.serviceWithZIO[Service](_.replay(replayFile))
  def extract(jsonFile: Path, commandsFile: Path) = ZIO.serviceWithZIO[Service](_.extract(jsonFile, commandsFile))
  def play(commandFile: Path, replayFile: Path) = ZIO.serviceWithZIO[Service](_.play(commandFile, replayFile))

  def live = (ServiceLive.apply _).toLayer

}

final case class ServiceLive(
    files: File,
    repository: Repository,
    console: Console,
    clock: Clock
) extends Service {

  def convertToJson(
      scriptLogPath: Path,
      scriptFilePath: Path,
      jsonFilePath: Path
  ) = for {
    scriptLog <- repository.scriptLog(scriptLogPath)
    scriptFile <- repository.scriptFile(scriptFilePath)
    jsonFile <- ReplayFile.fromFiles(scriptLog, scriptFile)
    _ <- files.writeFile(jsonFilePath, jsonFile.toJsonPretty)
  } yield ()

  def convertFromJson(
      scriptLogPath: Path,
      scriptFilePath: Path,
      jsonFilePath: Path
  ) = for {
    jsonFile <- files.replayFile(jsonFilePath)
    _ <- files.writeFile(scriptLogPath, jsonFile.scriptLog.toString)
    _ <- files.writeFile(scriptFilePath, jsonFile.scriptFile.toString)
  } yield ()

  def replay(replayFile: Path) = for {
    scriptFile <- files.replayFile(replayFile)
    _ <- ZStream.fromChunk(scriptFile.entries).foreach { case (delay, msg) =>
      clock
        .sleep(Duration.fromMillis((delay * 1000).toLong))
        .flatMap(_ => console.print(msg))
    }
  } yield ()

  def extract(jsonFile: Path, commandsFile: Path) = for {
    scriptFile <- files.replayFile(jsonFile)
    commands = scriptFile.entries
      .map(_._2)
      .mkString
      .split("\r\n")
      .filter(_.startsWith("scala"))
    _ <- ZIO.foreach(Chunk.fromArray(commands)) { command =>
      console.printLine(command)
    }
  } yield ()

  private def dumpProcess(outqueue: Queue[Chunk[Byte]], process: Process) =
    process.stdout.stream.foreach(s =>
      for {
        _ <- outqueue.offer(Chunk(s))
        _ <- console.print(new String(Array(s)))
      } yield ()
    )

  private def dumpErrProcess(process: Process) =
    process.stderr.stream.foreach(s => console.print(new String(Array(s))))

  private def tochunk(s: String) =
    Chunk.fromArray(s.getBytes(StandardCharsets.ISO_8859_1))

  private def write(
      command: String,
      queue: Queue[Chunk[Byte]],
      outqueue: Queue[Chunk[Byte]]
  ) = for {
    _ <- console.printLine(command)
    _ <- outqueue.offer(tochunk(s"$command\n"))
    _ <- queue.offer(tochunk(s"$command\n"))
    _ <- clock.sleep(1000.millis)
  } yield ()

  private def writeAll(
      commands: Chunk[String],
      queue: Queue[Chunk[Byte]],
      outqueue: Queue[Chunk[Byte]]
  ): ZIO[Any, Throwable, Unit] = commands.headOption match {
    case Some(command) =>
      for {
        _ <- write(command, queue, outqueue)
        _ <- writeAll(commands.tail, queue, outqueue)
      } yield ()
    case None => ZIO.unit
  }

  def play(commandFile: Path, replayFile: Path) = for {
    commands <- files.fromJsonFile[Commands](commandFile)
    outqueue <- Queue.unbounded[Chunk[Byte]]
    queue <- Queue.unbounded[Chunk[Byte]]
    process <- Command("scala", "-Dscala.color")
      .stdin(ProcessInput.fromQueue(queue))
      .run
    fiber <- dumpProcess(outqueue, process).fork
    _ <- clock.sleep(7000.millis)
    _ <- writeAll(commands.commands, queue, outqueue)
    _ <- clock.sleep(1000.millis)
    size <- outqueue.size
    _ <- console.printLine(s"size: $size")
    output <- outqueue.takeAll
    content = new String(output.flatten.toArray)
    _ <- console.printLine(s"----\n$content")
    _ <- queue.shutdown
  } yield ()

}
