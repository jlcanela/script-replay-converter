import mill._, scalalib._

object converter extends ScalaModule {

  val a = 1
  
  val ZIO_V2 = "2.0.0-RC3"

  def scalaVersion = "3.1.0"
  //def scalaVersion = "2.13.7"

  def ivyDeps = Agg(
    ivy"dev.zio::zio::${ZIO_V2}",
    ivy"dev.zio::zio-json::0.3.0-RC4",
    ivy"dev.zio::zio-streams::${ZIO_V2}",
    ivy"dev.zio::zio-process:0.7.0-RC3",
//    ivy"dev.zio::zio-cli:0.2.2",
    ivy"dev.zio::zio-cli:0.2.2+1-9d5db0e7-SNAPSHOT"
    
  )

  override def mainClass = T { Some("ConverterCli") }

}