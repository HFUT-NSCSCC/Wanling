package myCPU.pipeline.execute

import spinal.core._
import spinal.lib._
import myCPU.builder.Plugin
import myCPU.core.Core
import myCPU.constants.ALUOpType
import myCPU.pipeline.fetch.PCManagerPlugin
import myCPU.constants.ALUOpSrc

class IntALUPlugin extends Plugin[Core]{
    override def setup(pipeline: Core): Unit = {

    }
  
    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        EXE1 plug new Area{
            import EXE1._
            val aluSignals = input(exeSignals.intALUSignals)

            val IntALUOp = aluSignals.ALUOp
            val src1 = Select(
                (aluSignals.SRC1_FROM === ALUOpSrc.REG) -> U(aluSignals.SRC1),
                (aluSignals.SRC1_FROM === ALUOpSrc.IMM) -> U(aluSignals.IMM),
                (aluSignals.SRC1_FROM === ALUOpSrc.PC)  -> U(input(fetchSignals.PC)),
                default -> U(0, 32 bits)
            )
            val src2 = Select(
                (aluSignals.SRC2_FROM === ALUOpSrc.REG) -> U(aluSignals.SRC2),
                (aluSignals.SRC2_FROM === ALUOpSrc.IMM) -> U(aluSignals.IMM),
                (aluSignals.SRC2_FROM === ALUOpSrc.PC)  -> U(input(fetchSignals.PC)),
                default -> U(0, 32 bits)
            )
            val sa = src2(4 downto 0)
            val result = UInt(32 bits)
            switch(IntALUOp){
                import myCPU.constants.ALUOpType._
                is(ADD){
                    result := src1 + src2
                }
                is(SUB){
                    result := src1 - src2
                }
                is(XOR){
                    result := src1 ^ src2
                }
                is(AND){
                    result := src1 & src2
                }
                is(OR){
                    result := src1 | src2
                }
                is(NOR){
                    result := ~(src1 | src2)
                }
                is(SLT){
                    result := (src1.asSInt < src2.asSInt).asUInt.resize(32 bits)
                }
                is(SLTU){
                    result := (src1 < src2).asUInt.resize(32 bits)
                }
                is(SLL){
                    result := (src1 |<< sa)
                }
                is(SRL){
                    result := (src1 |>> sa)
                }
                is(SRA){
                    result := (src1.asSInt |>> sa).asUInt
                }
                is(LU12I){
                    result := src1 |<< 12
                }
                // default{
                //     result := 0
                // }
            }
        
            insert(writeSignals.ALU_RESULT) := result.asBits
        }
    }
}
