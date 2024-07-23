package myCPU.pipeline.decode

import _root_.myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import _root_.myCPU.constants.BRUOpType
import myCPU.constants.JumpType
import myCPU.constants.FuType
import myCPU.constants.OpSrc
import _root_.myCPU.core.RegFilePlugin

class ScoreBoardPlugin extends Plugin[Core]{
    // 一个32位的记分牌, 记录每个寄存器的状态
    val scoreBoard = Vec(RegInit(False), 32)
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        val setScoreboard = ISS plug new Area{
            import ISS._


            val regWriteAddr = input(decodeSignals.REG_WRITE_ADDR).asUInt
            // 确保该指令会修改寄存器：
            // 指令为ALU指令或为BL指令
            val setValid = (input(decodeSignals.REG_WRITE_VALID)) && (regWriteAddr =/= 0) && (arbitration.isValidNotStuck)
            when(setValid){
                scoreBoard(regWriteAddr) := True
            }

            val regfile = service(classOf[RegFilePlugin])

            arbitration.haltItself setWhen(
                ((scoreBoard(output(decodeSignals.SRC1Addr).asUInt) 
                        && (input(decodeSignals.SRC1_FROM) === OpSrc.REG) && !regfile.rs1Forwardable) 
                || 
                (scoreBoard(output(decodeSignals.SRC2Addr).asUInt) 
                        && (input(decodeSignals.SRC2_FROM) === OpSrc.REG) && !regfile.rs2Forwardable) ))
        }

        EXE2 plug new Area{
            import EXE2._
            val regWriteAddr = input(decodeSignals.REG_WRITE_ADDR).asUInt
            val clrValid = input(decodeSignals.REG_WRITE_VALID) && regWriteAddr =/= 0 && arbitration.isValidNotStuck
            when(clrValid && !setScoreboard.setValid){
                scoreBoard(regWriteAddr) := False
            }
        }
    }
  
}
