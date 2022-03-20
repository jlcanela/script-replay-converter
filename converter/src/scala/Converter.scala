import zio._ 
import zio.json._
import zio.stream._

import java.nio.file.{Path => JPath}


trait Converter {
    def toJson(scriptLogPath: JPath, scriptFile: JPath, jsonFile: JPath): ZIO[Console & Files, Throwable, Unit]
    def fromJson(scriptLogPath: JPath, scriptFile: JPath, jsonFile: JPath): ZIO[Console & Files, Throwable, Unit]

}

object Converter {

    def toJson(scriptLogPath: JPath, scriptFile: JPath, jsonFile: JPath) = ZIO.serviceWithZIO[Converter](_.toJson(scriptLogPath, scriptFile, jsonFile))
    def fromJson(scriptLogPath: JPath, scriptFile: JPath, jsonFile: JPath) = ZIO.serviceWithZIO[Converter](_.fromJson(scriptLogPath, scriptFile, jsonFile))

    val live = (ConverterLive.apply _).toLayer
}

final case class ConverterLive() extends Converter {
    def toJson(scriptLogPath: JPath, scriptFilePath: JPath, jsonFilePath: JPath): ZIO[Console & Files, Throwable, Unit] = for {
         scriptLog <- Files.scriptLog(scriptLogPath)
         scriptFile <- Files.scriptFile(scriptFilePath)
         jsonFile = JsonFile.fromFiles(scriptLog, scriptFile)
         _ <- Files.writeFile(jsonFilePath, jsonFile.toJsonPretty)
    } yield () 
    def fromJson(scriptLogPath: JPath, scriptFilePath: JPath, jsonFilePath: JPath): ZIO[Console & Files, Throwable, Unit] = for {
        jsonFile <- Files.jsonFile(jsonFilePath)
        scriptLog <- Files.scriptLogFromJson(jsonFile)
        scriptFile <- Files.scriptFileFromJson(jsonFile)
        _ <- Files.writeFile(scriptLogPath, scriptLog.toString)
        _ <- Files.writeFile(scriptFilePath, scriptFile.toString)
    } yield ()

}
