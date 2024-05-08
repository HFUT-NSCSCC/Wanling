package myCPU.core

import spinal.core._
import myCPU.builder._
import myCPU.pipeline.decode.test
import myCPU.pipeline.fetch.TestInst
import myCPU.pipeline.execute.TestALU

case class CoreConfig(){
    object RD extends Stageable(Bits(32 bits))
    object RJ extends Stageable(Bits(32 bits))
    object RK extends Stageable(Bits(32 bits))
    object RA extends Stageable(Bits(32 bits))
    object INST extends Stageable(Bits(32 bits))
    object REG_WRITE_VALID extends Stageable(Bool)
    object REG_WRITE_ADDR extends Stageable(Bits(32 bits))
    object REG_WRITE_DATA extends Stageable(Bits(32 bits))
}


class Core(val config: CoreConfig) extends Component with Pipeline {
    val io = new Bundle{

    }
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
        new TestInst,
        new TestALU
    )
    // this.connectStages()
}
