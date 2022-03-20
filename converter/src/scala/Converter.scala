import zio._ 
import zio.json._
import zio.stream._

import java.nio.file.{Path}


trait Converter {
    def toJson(scriptLogPath: Path, scriptFile: Path, jsonFile: Path): ZIO[Console & Files, Throwable, Unit]
    def fromJson(scriptLogPath: Path, scriptFile: Path, jsonFile: Path): ZIO[Console & Files, Throwable, Unit]

}

object Converter {

    def toJson(scriptLogPath: Path, scriptFile: Path, jsonFile: Path) = ZIO.serviceWithZIO[Converter](_.toJson(scriptLogPath, scriptFile, jsonFile))
    def fromJson(scriptLogPath: Path, scriptFile: Path, jsonFile: Path) = ZIO.serviceWithZIO[Converter](_.fromJson(scriptLogPath, scriptFile, jsonFile))

    val live = (ConverterLive.apply _).toLayer
}

final case class ConverterLive() extends Converter {
    def toJson(scriptLogPath: Path, scriptFilePath: Path, jsonFilePath: Path): ZIO[Console & Files, Throwable, Unit] = for {
         scriptLog <- Files.scriptLog(scriptLogPath)
         scriptFile <- Files.scriptFile(scriptFilePath)
         jsonFile = JsonFile.fromFiles(scriptLog, scriptFile)
         _ <- Files.writeFile(jsonFilePath, jsonFile.toJsonPretty)
    } yield () 
    def fromJson(scriptLogPath: Path, scriptFilePath: Path, jsonFilePath: Path): ZIO[Console & Files, Throwable, Unit] = for {
        jsonFile <- Files.jsonFile(jsonFilePath)
        scriptLog <- Files.scriptLogFromJson(jsonFile)
        scriptFile <- Files.scriptFileFromJson(jsonFile)
        _ <- Files.writeFile(scriptLogPath, scriptLog.toString)
        _ <- Files.writeFile(scriptFilePath, scriptFile.toString)
    } yield ()

}
