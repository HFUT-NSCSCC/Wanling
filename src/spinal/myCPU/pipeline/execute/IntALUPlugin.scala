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
            // ----- 有优先级 -----
            // val src1 = Select(
            //     (aluSignals.SRC1_FROM === ALUOpSrc.REG) -> U(aluSignals.SRC1),
            //     (aluSignals.SRC1_FROM === ALUOpSrc.IMM) -> U(aluSignals.IMM),
            //     (aluSignals.SRC1_FROM === ALUOpSrc.PC)  -> U(input(fetchSignals.PC)),
            //     default -> U(0, 32 bits)
            // )
            // val src2 = Select(
            //     (aluSignals.SRC2_FROM === ALUOpSrc.REG) -> U(aluSignals.SRC2),
            //     (aluSignals.SRC2_FROM === ALUOpSrc.IMM) -> U(aluSignals.IMM),
            //     (aluSignals.SRC2_FROM === ALUOpSrc.PC)  -> U(input(fetchSignals.PC)),
            //     default -> U(0, 32 bits)
            // )

            // ----- 无优先级 -----
            val src1 = UInt(32 bits)
            val src2 = UInt(32 bits)
            switch(aluSignals.SRC1_FROM) {
                is(ALUOpSrc.REG) {
                    src1 := U(aluSignals.SRC1)
                }
                is(ALUOpSrc.IMM) {
                    src1 := U(aluSignals.IMM)
                }
                is(ALUOpSrc.PC) {
                    src1 := U(input(fetchSignals.PC))
                }
                default {
                    src1 := U(0, 32 bits)
                }
            }

            switch(aluSignals.SRC2_FROM) {
                is(ALUOpSrc.REG) {
                    src2 := U(aluSignals.SRC2)
                }
                is(ALUOpSrc.IMM) {
                    src2 := U(aluSignals.IMM)
                }
                is(ALUOpSrc.PC) {
                    src2 := U(input(fetchSignals.PC))
                }
                default {
                    src2 := U(0, 32 bits)
                }
            }
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
                is(MUL){
                    result := (src1.asSInt * src2.asSInt).asUInt(31 downto 0)
                }
                is(LU12I){
                    result := src1
                }
                // default{
                //     result := 0
                // }
            }
        
            insert(writeSignals.ALU_RESULT) := result.asBits
        }
    }
}
