import zio.ZIOAppArgs

import zio.cli.{Args, CliApp, Command, Exists, HelpDoc, Options}
import zio.cli.HelpDoc.Span.text

import zio.ZIOAppDefault
import zio.Console.printLine

import java.nio.file.{Path}

import repositories._
import models._
import service._

object ConverterCli extends ZIOAppDefault {

  sealed trait Subcommand
  object Subcommand {
    final case class ToJson(scriptLog: Path, scriptFile: Path, json: Path) extends Subcommand
    final case class FromJson(scriptLog: Path, scriptFile: Path, json: Path) extends Subcommand
    final case class Replay(json: Path) extends Subcommand
    final case class Extract(json: Path, commands: Path) extends Subcommand
    final case class Play(json: Path, output: Path) extends Subcommand
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

  val extractHelp: HelpDoc = HelpDoc.p("<jsonFile> <commandsFile>: extract commands from <jsonfile> and save into <commandsFile>")
  val extract =
    Command("extract", Options.none, Args.file("jsonFile") ++ Args.file("commandsFile")).withHelp(extractHelp).map { 
        case (jsonFile, commandsFile) => Subcommand.Extract(jsonFile, commandsFile)
    }

  val playFileHelp: HelpDoc = HelpDoc.p("<playFile>: run commands file <commandfile> <replayFile>")
  val playFile =
    Command("play", Options.none, Args.file("playFile") ++ Args.file("replayFile")).withHelp(playFileHelp).map { 
        case (playFile, replayFile) => Subcommand.Play(playFile, replayFile)
    }

  val convert: Command[Subcommand] =
    Command("script-convert", Options.none, Args.none).subcommands(toJson, fromJson, replay, extract, playFile)

  val summary = """a converter to edit script screen capture
- to capture: "script --t=script_log -q scriptfile"
- to replay: "scriptreplay --timing=script_log scriptfile""""

  val converterApp = CliApp.make(
    name = "Script format converter",
    version = "1.0.0",
    summary = text(summary),
    command = convert
  ) {
    case Subcommand.FromJson(a, b, c) => Service.convertFromJson(a, b, c).tapError(zio.Console.printLine(_))
    case Subcommand.ToJson(a, b, c) => Service.convertToJson(a, b, c).tapError(zio.Console.printLine(_))
    case Subcommand.Replay(jsonfile) => Service.replay(jsonfile).tapError(zio.Console.printLine(_))
    case Subcommand.Extract(jsonFile, commandsFile) => Service.extract(jsonFile, commandsFile).tapError(zio.Console.printLine(_))
    case Subcommand.Play(commandFile, output) => Service.play(commandFile, output).tapError(zio.Console.printLine(_))
  }

  override def run =
    (for {
      args <- ZIOAppArgs.getArgs
      _    <- converterApp.run(args.toList)
        .provideCustom(
          Files.live, 
          Repository.live,
          Service.live)
    } yield ())
}
