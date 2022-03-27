package repositories

import zio._
import zio.json._
import zio.stream._

import java.io.PrintWriter
import java.nio.file.Path

import model._
import Model._

trait File {
    /*
    def read(path: Path): ZStream[Any, Throwable, Byte]
    def readString(path: Path): ZIO[Any, Throwable, String]
    */
    
    def stringStream(path: Path): ZStream[Any, Throwable, String]
    def fromJsonFile[T](path: Path)(implicit T: JsonDecoder[T]): ZIO[Any, Throwable, T]    
    def replayFile(path: Path): ZIO[Any, Throwable, ReplayFile]
    def writeFile(path: Path, content: String): ZIO[Any, Throwable, Unit]
}

object File {

    /*
    
    def read(path: Path) = ZIO.serviceWith[File](_.read(path))
    def readString(path: Path) = ZIO.serviceWithZIO[File](_.readString(path))
    */
    
    def stringStream(path: Path) = ZIO.serviceWith[File](_.stringStream(path))
    def fromJsonFile[T](path: Path)(implicit T: JsonDecoder[T]) = ZIO.serviceWithZIO[File](_.fromJsonFile(path))
    def replayFile(path: Path) = ZIO.serviceWithZIO[File](_.replayFile(path))
    def writeFile(path: Path, content: String) = ZIO.serviceWithZIO[File](_.writeFile(path, content))

    val live = (FileLive.apply _).toLayer
}

case class FileLive(console: Console) extends File {

    private def read(path: Path) =  ZStream.fromFile(path.toFile)    
    private def readString(path: Path) = ZIO.attempt(scala.io.Source.fromFile(path.toFile).getLines.mkString)
    
    def stringStream(path: Path) = read(path)
    .via(ZPipeline.iso_8859_1Decode)
    .via(ZPipeline.splitLines)
    
    def fromJsonFile[T](path: Path)(implicit T: JsonDecoder[T]) =  for {
        content <- readString(path)
        either <- ZIO.attempt(content.fromJson[T]).tapError(console.printLine(_))
        o <- ZIO.fromEither(either).mapError(error => new Exception(error))
    } yield o
    
    
    def writeFile(path: Path, content: String) = ZIO.attempt {
        val writer = new PrintWriter(path.toFile)
        writer.write(content)
        writer.close()
    }
    
    def replayFile(path: Path) = fromJsonFile[ReplayFile](path)
    
}
