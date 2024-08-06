package myCPU.pipeline.issue

import _root_.myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import _root_.myCPU.constants.BRUOpType
import myCPU.constants.JumpType
import myCPU.constants.FuType
import myCPU.constants.OpSrc
import _root_.myCPU.core.RegFilePlugin
import _root_.myCPU.pipeline.decode

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


            val regWriteAddr = input(decodeSignals.REG_WRITE_ADDR)
            // 确保该指令会修改寄存器：
            // 指令为ALU指令或为BL指令
            val setValid = (input(decodeSignals.REG_WRITE_VALID)) && (regWriteAddr =/= 0) && (arbitration.isValidNotStuck)
            when(setValid){
                scoreBoard(regWriteAddr) := True
            }

            val regfile = service(classOf[RegFilePlugin])

            arbitration.haltItself setWhen(
                ((scoreBoard(output(decodeSignals.SRC1Addr)) 
                        && (input(decodeSignals.SRC1_FROM) === OpSrc.REG) && !regfile.rs1Forwardable) 
                || 
                (scoreBoard(output(decodeSignals.SRC2Addr)) 
                        && (input(decodeSignals.SRC2_FROM) === OpSrc.REG) && !regfile.rs2Forwardable) ))
        }

        EXE3 plug new Area{
            import EXE3._
            val regWriteAddrEXE3 = input(decodeSignals.REG_WRITE_ADDR)
            val regWriteAddrEXE2 = EXE2.input(decodeSignals.REG_WRITE_ADDR)
            val regWriteAddrEXE1 = EXE1.input(decodeSignals.REG_WRITE_ADDR)
            val regWriteValidEXE3 = input(decodeSignals.REG_WRITE_VALID)
            val regWriteValidEXE2 = EXE2.input(decodeSignals.REG_WRITE_VALID)
            val regWriteValidEXE1 = EXE1.input(decodeSignals.REG_WRITE_VALID)
            // exe2 与 exe1 写入的目标不一致, 才可以清除
            val clrValid = (regWriteValidEXE3 && regWriteAddrEXE3 =/= 0 && arbitration.isValidNotStuck) &&
                            ((regWriteValidEXE2 ^ regWriteValidEXE3) || (regWriteAddrEXE2 =/= regWriteAddrEXE3)) &&
                            ((regWriteValidEXE1 ^ regWriteValidEXE3) || (regWriteAddrEXE1 =/= regWriteAddrEXE3))
            // val clrValid = input(decodeSignals.REG_WRITE_VALID) && regWriteAddrEXE3 =/= 0 && arbitration.isValidNotStuck
            when(clrValid && !(setScoreboard.setValid && setScoreboard.regWriteAddr === regWriteAddrEXE3)){
                scoreBoard(regWriteAddrEXE3) := False
            }
        }
    }
  
}
