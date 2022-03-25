package model

import zio._
import zio.json._

import repositories._

object Model {
    
    final case class ReplayFile(header: String, footer: String, entries: Chunk[(Double, String)]) {
        def scriptLog = ScriptLog(entries.map(x => (x._1, x._2.length)))
        def scriptFile = ScriptFile(header, footer, entries.map(_._2).mkString)
    }

    object ReplayFile {
        implicit val encoder: JsonEncoder[ReplayFile] = DeriveJsonEncoder.gen[ReplayFile]
        implicit val decoder: JsonDecoder[ReplayFile] = DeriveJsonDecoder.gen[ReplayFile]

        def fromFiles(scriptLog: ScriptLog, scriptFile: ScriptFile): ZIO[Any, Throwable, ReplayFile] = {
            
            def extractLine(offset: Int, entry: ScriptEntry) = (
                offset + entry._2,
                (entry._1,  scriptFile.body.drop(offset).take(entry._2))
                )
                
            if (scriptLog.items.map(_._2).sum == scriptFile.body.size) {
                ZIO.fail(new Exception(s"expected size: ${scriptLog.items.map(_._2).sum} found: ${scriptFile.body.size}"))
            } else {
                ZIO.succeed(ReplayFile(scriptFile.header, scriptFile.footer, scriptLog.items.mapAccum(0)(extractLine)._2))
            }
        }
    }

    type ScriptEntry = (Double, Int)
    
    final case class ScriptLog(items: Chunk[ScriptEntry]) {
        override def toString() = items.map(fe => f"${fe._1}%4.6f ${fe._2}%s").mkString("", "\n", "\n")
    }
    
    final case class ScriptFile(header: String, footer: String, body: String) {
        override def toString() = Array(header, body, footer, "").mkString("\r\n")
    }

    object ScriptFile {

        def fromChunk(chunk: Chunk[String]) = {
            val nbLines = chunk.size
            ScriptFile(chunk(0), chunk(nbLines-1), chunk.drop(1).take(nbLines-2).mkString("\r\n"))
        }

    }

    case class Commands(commands: Chunk[String])

    object Commands {
        implicit val encoder: JsonEncoder[Commands] = DeriveJsonEncoder.gen[Commands]
        implicit val decoder: JsonDecoder[Commands] = DeriveJsonDecoder.gen[Commands]
    }

}
