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

trait Service {

  def convertFromJson(a: Path, b: Path, c: Path): UIO[Unit]
  def convertToJson(a: Path, b: Path, c: Path): UIO[Unit]
  def replay(replayFile: Path): UIO[Unit]
  def extract(jsonFile: Path, commandsFile: Path): UIO[Unit]
  def play(commandFile: Path, replayFile: Path): ZIO[Any, Throwable, Unit]

}

object Service {

  def convertFromJson(a: Path, b: Path, c: Path) = ZIO.serviceWithZIO[Service](_.convertFromJson(a, b, c))
  def convertToJson(a: Path, b: Path, c: Path) = ZIO.serviceWithZIO[Service](_.convertToJson(a, b, c))
  def replay(replayFile: Path) = ZIO.serviceWithZIO[Service](_.replay(replayFile))
  def extract(jsonFile: Path, commandsFile: Path) = ZIO.serviceWithZIO[Service](_.extract(jsonFile, commandsFile))
  def   play(commandFile: Path, replayFile: Path) = ZIO.serviceWithZIO[Service](_.play(commandFile, replayFile))

  def live = (ServiceLive.apply _).toLayer

}

final case class ServiceLive(
    file: File,
    repository: Repository,
    shellProcess: ShellProcess,
    console: Console,
    clock: Clock
) extends Service {

  def convertToJson(
      scriptLogPath: Path,
      scriptFilePath: Path,
      jsonFilePath: Path
  ) = (for {
    scriptLog <- repository.scriptLog(scriptLogPath)
    scriptFile <- repository.scriptFile(scriptFilePath)
    jsonFile <- ReplayFile.fromFiles(scriptLog, scriptFile)
    _ <- file.writeFile(jsonFilePath, jsonFile.toJsonPretty)
  } yield ()).orDie

  def convertFromJson(
      scriptLogPath: Path,
      scriptFilePath: Path,
      jsonFilePath: Path
  ) = (for {
    jsonFile <- file.replayFile(jsonFilePath)
    _ <- file.writeFile(scriptLogPath, jsonFile.scriptLog.toString)
    _ <- file.writeFile(scriptFilePath, jsonFile.scriptFile.toString)
  } yield ()).orDie

  def replay(replayFile: Path) = (for {
    scriptFile <- file.replayFile(replayFile)
    _ <- ZStream.fromChunk(scriptFile.entries).foreach { case (delay, msg) =>
      clock
        .sleep(Duration.fromMillis((delay * 1000).toLong))
        .flatMap(_ => console.print(msg))
    }
  } yield ()).orDie

  def extract(jsonFile: Path, commandsFile: Path) = (for {
    scriptFile <- file.replayFile(jsonFile)
    commands = scriptFile.entries
      .map(_._2)
      .mkString
      .split("\r\n")
      .filter(_.startsWith("scala"))
    _ <- ZIO.foreach(Chunk.fromArray(commands)) { command =>
      console.printLine(command)
    }
  } yield ()).orDie

  def play(commandFile: Path, replayFilePath: Path) = (for {
    commands <- file.fromJsonFile[Commands](commandFile)
    replayFile <- shellProcess.run(ShellProcess.Shell.scala, commands.commands)
   // replayFile <- service.Shell(Array("scala", "-Dscala.color"), commands.commands).run()
    content = replayFile.toJsonPretty
     _ <- file.writeFile(replayFilePath, content)
  } yield ()).orDie

}
