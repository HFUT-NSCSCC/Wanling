package myCPU.core

import spinal.core._
import myCPU.builder._
import myCPU.pipeline.fetch.PCManagerPlugin
import myCPU.pipeline.decode.DecoderPlugin
import myCPU.pipeline.execute.IntALUPlugin
import myCPU.pipeline.execute.LSUPlugin
import myCPU.constants._
import myCPU.constants.LA32._
import myCPU.pipeline.execute.BRUPlugin
import myCPU.pipeline.fetch.FetchSignals
import myCPU.pipeline.execute.ExecuteSignals
import myCPU.pipeline.decode.DecodeSignals
import myCPU.pipeline.writeback.WritebackSignals
import _root_.myCPU.pipeline.issue.IntIssuePlugin
import myCPU.pipeline.issue.BruIssuePlugin
import myCPU.pipeline.issue.LsuIssuePlugin
import _root_.myCPU.pipeline.fetch.FetcherPlugin
import myCPU.pipeline.decode.ScoreBoardPlugin
import _root_.myCPU.pipeline.issue.IssuePlugin

case class CoreConfig(){
    // object PC extends Stageable(Bits(PCWidth bits))
    // object RJ extends Stageable(Bits(5 bits))
    // object RK extends Stageable(Bits(5 bits))
    // object RJData extends Stageable(Bits(32 bits))
    // object RKData extends Stageable(Bits(32 bits))

    // object FUType extends Stageable(FuType())
    // object IMM extends Stageable(Bits(DataWidth bits))
    // object IMMExtType extends Stageable(ImmExtType())
    // object JUMPType extends Stageable(JumpType())

    // // ALU
    // object SRC1Addr extends Stageable(Bits(RegAddrWidth bits))
    // object SRC2Addr extends Stageable(Bits(RegAddrWidth bits))
    // object SRC1 extends Stageable(Bits(DataWidth bits))
    // object SRC2 extends Stageable(Bits(DataWidth bits))
    // object ALUOp extends Stageable(ALUOpType())
    // object SRC1_FROM_IMM extends Stageable(Bool)
    // object SRC2_FROM_IMM extends Stageable(Bool)
    // object RESULT extends Stageable(Bits(DataWidth bits))

    // // BRU
    // object BRUOp extends Stageable(BRUOpType())

    // // LSU
    // object MEM_READ extends Stageable(Bits(4 bits))
    // object MEM_READ_UE extends Stageable(Bool)
    // object MEM_WRITE extends Stageable(Bits(4 bits))
    // object MEM_RDATA extends Stageable(Bits(DataWidth bits))
    // // object RA extends Stageable(Bits(32 bits))
    // // object INST extends Stageable(Bits(InstWidth bits))
    // object REG_WRITE_VALID extends Stageable(Bool)
    // object REG_WRITE_ADDR extends Stageable(Bits(RegAddrWidth bits))
    // object REG_WRITE_DATA extends Stageable(Bits(DataWidth bits))
}

class Core(val config: CoreConfig) extends Component with Pipeline {
    // val io = new Bundle{
    //     // val ipi = in(Bool)
    //     // val interrupt = in(Bits(2 bits))

    //     // val inst = new Bundle{
    //     //     val en = out(Bool)
    //     //     val addr = out(Bits(32 bits))
    //     //     val rdata = in(Bits(32 bits))
    //     // }

    //     // val data = new Bundle{
    //     //     val en = out(Bool)
    //     //     val wen = out(Bool)
    //     //     val addr = out(Bits(32 bits))
    //     //     val wdata = out(Bits(32 bits))
    //     //     val rdata = in(Bits(32 bits))
    //     // }

    //     val debug = new Bundle{
    //         val pc = out(Bits(32 bits))
    //         val wen = out(Bool)
    //         val wnum = out(Bits(5 bits))
    //         val wdata = out(Bits(32 bits))
    //     }
    // }
    type T = Core
    import config._

    def newStage(): Stage = {val s = new Stage; stages += s; s}

    val IF1  = newStage()
    val IF2  = newStage()
    val ID   = newStage()
    val ISS  = newStage()
    val EXE1 = newStage()
    val EXE2 = newStage()
    // val EXE3 = newStage()
    val WB   = newStage()

    val fetchSignals = new FetchSignals(config)
    val decodeSignals = new DecodeSignals(config)
    val exeSignals = new ExecuteSignals(config)
    val writeSignals = new WritebackSignals(config)

    plugins ++= List(
        new RegFilePlugin,
        new PCManagerPlugin,
        new FetcherPlugin,
        new DecoderPlugin,
        new ScoreBoardPlugin,
        // new IssuePlugin,
        new IntIssuePlugin,
        // new BruIssuePlugin,
        new LsuIssuePlugin,
        new IntALUPlugin,
        new LSUPlugin,
        new BRUPlugin,
    )

    // val regFile = service(classOf[RegFilePlugin])
    // io.debug.pc := 0x0
    // io.debug.wen := regFile.debug.wen
    // io.debug.wnum := regFile.debug.wnum
    // io.debug.wdata := regFile.debug.wdata
    // this.connectStages()
}
