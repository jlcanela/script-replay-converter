import zio._
import zio.json._

object JsonFile {
    
    final case class ScriptFile(header: String, footer: String, entries: Chunk[(Double, String)])

    object ScriptFile {
        implicit val encoder: JsonEncoder[ScriptFile] = DeriveJsonEncoder.gen[ScriptFile]
        implicit val decoder: JsonDecoder[ScriptFile] = DeriveJsonDecoder.gen[ScriptFile]
    }

    def fromFiles(scriptLog: Files.ScriptLog, scriptFile: Files.ScriptFile): ScriptFile = {
        
        assert(scriptLog.items.map(_._2).sum == scriptFile.body.size)

        def extractLine(offset: Int, entry: Files.ScriptEntry) = (
            offset + entry._2,
            (entry._1,  scriptFile.body.drop(offset).take(entry._2))
        )

        ScriptFile(scriptFile.header, scriptFile.footer, scriptLog.items.mapAccum(0)(extractLine)._2)
    }
}
    