import mill._, scalalib._

object converter extends ScalaModule {

  val a = 1
  
  val ZIO_V2 = "2.0.0-RC1"

  def scalaVersion = "3.1.0"

  def ivyDeps = Agg(
    ivy"dev.zio::zio::${ZIO_V2}",
    ivy"dev.zio::zio-json::0.3.0-RC1-1",
    ivy"dev.zio::zio-streams::${ZIO_V2}",
    ivy"dev.zio::zio-cli:0.2.2"
  )

  override def mainClass = T { Some("ConverterCli") }

}