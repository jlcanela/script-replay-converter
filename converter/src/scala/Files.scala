import zio._
import zio.json._
import zio.stream._

import java.io.PrintWriter
import java.nio.file.{Path, Files}

trait Files {
    def read(path: Path): ZStream[Any, Throwable, Byte]
    def writeFile(path: Path, content: String): ZIO[Any, Throwable, Unit]

    def scriptLog(path: Path): Task[Files.ScriptLog]
    def scriptFile(path: Path): ZIO[Console, Throwable, Files.ScriptFile]
    def jsonFile(path: Path): ZIO[Any, Throwable, JsonFile.ScriptFile]

    def scriptLogFromJson(file: JsonFile.ScriptFile): Files.ScriptLog
    def scriptFileFromJson(file: JsonFile.ScriptFile): Files.ScriptFile 
}

object Files {

    type ScriptEntry = (Double, Int)
    final case class ScriptLog(items: Chunk[Files.ScriptEntry]) {
        override def toString() = items.map(fe => f"${fe._1}%4.6f ${fe._2}%s").mkString("", "\n", "\n")
    }
    def scriptLog(path: Path) = ZIO.serviceWithZIO[Files](_.scriptLog(path))
    def scriptLogFromJson(file: JsonFile.ScriptFile) = ZIO.serviceWith[Files](_.scriptLogFromJson(file))
    
    final case class ScriptFile(header: String, footer: String, body: String) {
        override def toString() = Array(header, body, footer, "").mkString("\r\n")
    }
    def scriptFile(path: Path) = ZIO.serviceWithZIO[Files](_.scriptFile(path))
    def scriptFileFromJson(file: JsonFile.ScriptFile) = ZIO.serviceWith[Files](_.scriptFileFromJson(file))
    
    def writeFile(path: Path, content: String) = ZIO.serviceWithZIO[Files](_.writeFile(path, content))
    def jsonFile(path: Path) = ZIO.serviceWithZIO[Files](_.jsonFile(path))
    
    val live = (FilesLive.apply _).toLayer
}

case class FilesLive() extends Files {
    
    def read(path: Path) =  ZStream.fromFile(path.toFile)    
    
    def toScriptEntry(s: String) = ZIO.attempt {
        val Array(double: String, int: String) = s.split(" ")
        (double.toDouble, int.toInt)
    }
    
    def scriptLog(path: Path): ZIO[Any, Throwable, Files.ScriptLog] = for { 
        chunk <- read(path)
        .via(ZPipeline.iso_8859_1Decode)
        .via(ZPipeline.splitLines)
        .via(ZPipeline.mapZIO(toScriptEntry _))
        .run(ZSink.collectAll[Files.ScriptEntry])
    } yield Files.ScriptLog(chunk)
    
    def scriptFile(path: Path): ZIO[Console, Throwable, Files.ScriptFile] = for { 
        chunk <- read(path)
        .via(ZPipeline.iso_8859_1Decode)
        .via(ZPipeline.splitLines)
        .run(ZSink.collectAll[String])
        nbLines = chunk.size
        scriptFile = Files.ScriptFile(chunk(0), chunk(nbLines-1), chunk.drop(1).take(nbLines-2).mkString("\r\n"))
    } yield scriptFile
    
    def writeFile(path: Path, content: String) = ZIO.attempt {
        val writer = new PrintWriter(path.toFile)
        writer.write(content)
        writer.close()
    }
    
    def jsonFile(path: Path) = for {
        content <- ZIO.attempt(scala.io.Source.fromFile(path.toFile).getLines.mkString)
        scriptFile <- ZIO.fromEither(content.fromJson[JsonFile.ScriptFile])
        .mapError(error => new Exception(error))
    } yield scriptFile

    def scriptLogFromJson(file: JsonFile.ScriptFile) = Files.ScriptLog(file.entries.map(x => (x._1, x._2.length)))
    def scriptFileFromJson(file: JsonFile.ScriptFile) = Files.ScriptFile(file.header, file.footer, file.entries.map(_._2).mkString)
}
