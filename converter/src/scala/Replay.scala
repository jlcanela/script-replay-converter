import zio._
import zio.stream._

trait Replay {

    def replay(scriptFile: JsonFile.ScriptFile): ZIO[Console & Clock, Throwable, Unit]
}

object Replay {

    def replay(scriptFile: JsonFile.ScriptFile) = ZIO.serviceWithZIO[Replay](_.replay(scriptFile))

    val live = (ReplayLive.apply _).toLayer

}

case class ReplayLive() extends Replay {
    def replay(scriptFile: JsonFile.ScriptFile) = for {
        _ <- ZStream.fromChunk(scriptFile.entries).foreach { (delay, msg) =>
             Console.print(msg).delay(Duration.fromMillis((delay * 1000).toLong))
        }
    } yield ()
}
