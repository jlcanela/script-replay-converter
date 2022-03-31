import mill._, scalalib._

object converter extends ScalaModule {

  //def scalaVersion = "3.1.0"
  def scalaVersion = "2.13.7"

  def ivyDepsRC2 = Agg(
    ivy"dev.zio::zio::2.0.0-RC2",
    ivy"dev.zio::zio-json::0.3.0-RC3",
    ivy"dev.zio::zio-streams::2.0.0-RC2",
    ivy"dev.zio::zio-process:0.7.0-RC2",
    ivy"dev.zio::zio-cli:0.2.2",
    ivy"io.d11::zhttp:2.0.0-RC4"
  )

  def ivyDepsRC3 = Agg(
    ivy"dev.zio::zio::2.0.0-RC3",
    ivy"dev.zio::zio-json::0.3.0-RC4",
    ivy"dev.zio::zio-streams::2.0.0-RC3",
    ivy"dev.zio::zio-process:0.7.0-RC3",
    ivy"dev.zio::zio-cli:0.2.2+1-9d5db0e7-SNAPSHOT",
    ivy"io.d11::zhttp:2.0.0-RC4"
  )

  def ivyDeps = ivyDepsRC3 

  override def mainClass = T { Some("ConverterCli") }

}

object http extends ScalaModule {

  //def scalaVersion = "3.1.0"
  def scalaVersion = "2.13.7"

  def ivyDepsRC2 = Agg(
    ivy"dev.zio::zio::2.0.0-RC2",
    // ivy"dev.zio::zio-json::0.3.0-RC3",
    // ivy"dev.zio::zio-streams::2.0.0-RC2",
    // ivy"dev.zio::zio-process:0.7.0-RC2",
    ivy"dev.zio::zio-cli:0.2.2",
    ivy"io.d11::zhttp:2.0.0-RC4"
  )

  def ivyDeps = ivyDepsRC2

  override def mainClass = T { Some("Cli") }

}