package http

import zio._ 
import zhttp.http._
import zhttp.service.Server

trait HttpServer {
    def runServer: ZIO[Any, Throwable, Unit]
}

object HttpServer {

  def runServer = ZIO.serviceWithZIO[HttpServer](_.runServer)

  val live = (HttpServerLive.apply _).toLayer

}

case class HttpServerLive() extends HttpServer {
    
  val app = Http.text("Hello World !")

  def runServer = Server.start(8090, app)

}
