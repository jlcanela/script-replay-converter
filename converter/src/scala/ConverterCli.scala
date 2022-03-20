import zio.ZIOAppArgs

import java.nio.file.{Path => JPath}
import zio.cli.{Args, CliApp, Command, Exists, HelpDoc, Options}
import zio.cli.HelpDoc.Span.text

import zio.ZIOAppDefault
import zio.Console.printLine

object ConverterCli extends ZIOAppDefault {
  import java.nio.file.Path

  sealed trait Subcommand
  object Subcommand {
    final case class ToJson(scriptLog: JPath, scriptFile: JPath, json: JPath) extends Subcommand
    final case class FromJson(scriptLog: JPath, scriptFile: JPath, json: JPath) extends Subcommand
    final case class Replay(json: JPath) extends Subcommand
  }

  val toJsonHelp: HelpDoc = HelpDoc.p("<scriptLog> <scriptFile> <jsonFile>: oonvert scriptLog & scriptFile into jsonFile")
  val toJson =
    Command("toJson", Options.none, Args.file("scriptLog") ++ Args.file("scriptFile") ++ Args.file("jsonFile")).withHelp(toJsonHelp).map { 
        case (scriptLog, scriptFile, jsonFile) => Subcommand.ToJson(scriptLog, scriptFile, jsonFile)
    }

  val fromJsonHelp: HelpDoc = HelpDoc.p("<scriptLog> <scriptFile> <jsonFile>: oonvert jsonFile into scriptLog & scriptFile")
  val fromJson =
    Command("fromJson", Options.none, Args.file("scriptLog") ++ Args.file("scriptFile") ++ Args.file("jsonFile")).withHelp(fromJsonHelp).map { 
        case (scriptLog, scriptFile, jsonFile) => Subcommand.FromJson(scriptLog, scriptFile, jsonFile)
    }

  val replayHelp: HelpDoc = HelpDoc.p("<jsonFile>: replay <jsonfile>")
  val replay =
    Command("replay", Options.none, Args.file("jsonFile")).withHelp(replayHelp).map { 
        case (jsonFile) => Subcommand.Replay(jsonFile)
    }

  val convert: Command[Subcommand] =
    Command("script-convert", Options.none, Args.none).subcommands(toJson, fromJson, replay)

  val summary = """a converter to edit script screen capture
- to capture: "script --t=script_log -q scriptfile"
- to replay: "scriptreplay --timing=script_log scriptfile""""

  val converterApp = CliApp.make(
    name = "Script format converter",
    version = "1.0.0",
    summary = text(summary),
    command = convert
  ) {
    case Subcommand.ToJson(a, b, c) => Converter.toJson(a, b, c).tapError(zio.Console.printLine(_))
    case Subcommand.FromJson(a, b, c) => Converter.fromJson(a, b, c).tapError(zio.Console.printLine(_))
    case Subcommand.Replay(jsonfile) => for {
      scriptFile <- Files.jsonFile(jsonfile)
      _ <- Replay.replay(scriptFile)
    } yield ()
  }

  override def run =
    (for {
      args <- ZIOAppArgs.getArgs
      _    <- converterApp.run(args.toList)
        .provideCustom(Converter.live, Files.live, Replay.live)
    } yield ())
}
