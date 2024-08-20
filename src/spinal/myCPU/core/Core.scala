package myCPU.core

import spinal.core._
import myCPU.builder._
import myCPU.pipeline.fetch.PCManagerPlugin
import myCPU.pipeline.decode.DecoderPlugin
import myCPU.pipeline.execute.IntALUPlugin
import myCPU.pipeline.execute.LSUPlugin
import myCPU.constants._
import myCPU.core.LA32R._
import myCPU.pipeline.execute.BRUPlugin
import myCPU.pipeline.fetch.FetchSignals
import myCPU.pipeline.execute.ExecuteSignals
import myCPU.pipeline.decode.DecodeSignals
import myCPU.pipeline.writeback.WritebackSignals
import _root_.myCPU.pipeline.issue.IntIssuePlugin
import myCPU.pipeline.issue.BruIssuePlugin
import myCPU.pipeline.issue.LsuIssuePlugin
import _root_.myCPU.pipeline.fetch.FetcherPlugin
import myCPU.pipeline.issue.ScoreBoardPlugin
import _root_.myCPU.pipeline.issue.IssuePlugin
import myCPU.pipeline.execute.MulPlugin
import _root_.myCPU.pipeline.fetch.ICachePlugin
import myCPU.pipeline.fetch.BPUPlugin

// 实际并没有用上，可以移除
case class CoreConfig(){
}

class Core(val config: CoreConfig) extends Component with Pipeline {
    type T = Core
    import config._

    def newStage(): Stage = {val s = new Stage; stages += s; s}

    val IF1  = newStage()
    val IF2  = newStage()
    val ID   = newStage()
    val ISS  = newStage()
    val EXE1 = newStage()
    val EXE2 = newStage()
    val EXE3 = newStage()
    val WB   = newStage()

    val fetchSignals = new FetchSignals(config)
    val decodeSignals = new DecodeSignals(config)
    val exeSignals = new ExecuteSignals(config)
    val writeSignals = new WritebackSignals(config)

    // 插件列表
    plugins ++= List(
        new RegFilePlugin,
        new PCManagerPlugin,
        // new FetcherPlugin,
        new ICachePlugin,
        new DecoderPlugin,
        // new ScoreBoardPlugin,
        new IssuePlugin,
        new IntIssuePlugin,
        // new BruIssuePlugin,
        new LsuIssuePlugin,
        new IntALUPlugin,
        new MulPlugin,
        new LSUPlugin,
        new BPUPlugin,
        // new BRUPlugin,
    )
}
