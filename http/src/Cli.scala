import zio._
import http._

object Cli extends ZIOAppDefault {

    def run = HttpServer.runServer.provideCustomLayer(HttpServer.live)
    
}
