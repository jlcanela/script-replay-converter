package repositories

import zio._
import zio.json._
import zio.stream._

import java.io.PrintWriter
import java.nio.file.Path

import model._
import Model._

trait Repository {

    def scriptLog(path: Path): ZIO[Any, Throwable, ScriptLog]
    def scriptFile(path: Path): ZIO[Any, Throwable, ScriptFile] 
}

object Repository {

    def scriptLog(path: Path) = ZIO.serviceWithZIO[Repository](_.scriptLog(path))
    def scriptFile(path: Path) = ZIO.serviceWithZIO[Repository](_.scriptFile(path))

    val live = (RepositoryLive.apply _).toLayer
}

final case class RepositoryLive(files: File) extends Repository {

    private def toScriptEntry(s: String) = ZIO.attempt {
        val Array(double: String, int: String) = s.split(" ")
        (double.toDouble, int.toInt)
    }

    def scriptLog(path: Path) = for { 
        chunk <- files.stringStream(path)
        .via(ZPipeline.mapZIO(toScriptEntry _))
        .run(ZSink.collectAll[ScriptEntry])
    } yield ScriptLog(chunk)
    
    def scriptFile(path: Path) = for { 
        chunk <- files.stringStream(path).run(ZSink.collectAll[String])
        scriptFile = ScriptFile.fromChunk(chunk)        
    } yield scriptFile

}