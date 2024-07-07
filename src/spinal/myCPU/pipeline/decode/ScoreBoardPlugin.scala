package myCPU.pipeline.decode

import _root_.myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import _root_.myCPU.constants.BRUOpType
import myCPU.constants.JumpType
import myCPU.constants.FuType
import myCPU.constants.ALUOpSrc

class ScoreBoardPlugin extends Plugin[Core]{
    // 一个32位的记分牌, 记录每个寄存器的状态
    val scoreBoard = Vec(RegInit(False), 32)
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        ID plug new Area{
            import ID._


            val regWriteAddr = output(decodeSignals.REG_WRITE_ADDR).asUInt
            // 确保该指令会修改寄存器：
            // 指令为ALU指令或为BL指令
            val setValid = (output(decodeSignals.REG_WRITE_VALID)) && (regWriteAddr =/= 0) && (arbitration.isValidNotStuck)
            when(setValid){
                scoreBoard(regWriteAddr) := True
            }


            arbitration.haltItself setWhen(
                ((scoreBoard(output(decodeSignals.SRC1Addr).asUInt) 
                        && (output(decodeSignals.SRC1_FROM) === ALUOpSrc.REG)) 
                || 
                (scoreBoard(output(decodeSignals.SRC2Addr).asUInt) 
                        && (output(decodeSignals.SRC2_FROM) === ALUOpSrc.REG)) ))
        }

        EXE3 plug new Area{
            import EXE3._
            val regWriteAddr = input(decodeSignals.REG_WRITE_ADDR).asUInt
            val clrValid = input(decodeSignals.REG_WRITE_VALID) && regWriteAddr =/= 0 && arbitration.isValidNotStuck
            when(clrValid){
                scoreBoard(regWriteAddr) := False
            }
        }
    }
  
}
