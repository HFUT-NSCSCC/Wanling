package myCPU.core

import spinal.core._
import myCPU.builder._
import myCPU.pipeline.decode.test
import _root_.myCPU.constants.ALUOpType

case class CoreConfig(){
    object PC extends Stageable(Bits(32 bits))
    object RD extends Stageable(Bits(5 bits))
    object RJ extends Stageable(Bits(5 bits))
    object RK extends Stageable(Bits(5 bits))

    object SRC1 extends Stageable(Bits(32 bits))
    object SRC2 extends Stageable(Bits(32 bits))
    object ALUOp extends Stageable(ALUOpType())
    object RESULT extends Stageable(Bits(32 bits))
    // object RA extends Stageable(Bits(32 bits))
    object INST extends Stageable(Bits(32 bits))
    object REG_WRITE_VALID extends Stageable(Bool)
    object REG_WRITE_ADDR extends Stageable(Bits(32 bits))
    object REG_WRITE_DATA extends Stageable(Bits(32 bits))
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
    val EXE1 = newStage()
    val EXE2 = newStage()
    val EXE3 = newStage()
    val WB   = newStage()

    plugins ++= List(
        new RegFilePlugin,
    )

    // val regFile = service(classOf[RegFilePlugin])
    // io.debug.pc := 0x0
    // io.debug.wen := regFile.debug.wen
    // io.debug.wnum := regFile.debug.wnum
    // io.debug.wdata := regFile.debug.wdata
    // this.connectStages()
}
