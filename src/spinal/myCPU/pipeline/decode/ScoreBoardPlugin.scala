package myCPU.pipeline.decode

import _root_.myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._

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
            val regWriteAddr = input(fetchSignals.INST)(4 downto 0).asUInt
            when(output(decodeSignals.REG_WRITE_VALID) && regWriteAddr =/= 0){
                scoreBoard(regWriteAddr) := True
            }
            arbitration.haltItself setWhen(
                (scoreBoard(output(decodeSignals.SRC1Addr).asUInt) 
                        && !output(decodeSignals.SRC1_FROM_IMM)) 
                || 
                (scoreBoard(output(decodeSignals.SRC2Addr).asUInt) 
                        && !output(decodeSignals.SRC2_FROM_IMM)) )
        }

        EXE3 plug new Area{
            import EXE3._
            val regWriteAddr = input(fetchSignals.INST)(4 downto 0).asUInt
            when(input(decodeSignals.REG_WRITE_VALID) && regWriteAddr =/= 0){
                scoreBoard(regWriteAddr) := False
            }
        }
    }
  
}
