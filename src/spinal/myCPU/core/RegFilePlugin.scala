package myCPU.core

import myCPU.builder.Plugin
import spinal.core._
import spinal.lib._
import myCPU._
import myCPU.constants._
import myCPU.constants.LA32._
import _root_.myCPU.constants.JumpType._
import _root_.myCPU.constants.FuType._
import myCPU.constants.JumpType._

class RegFilePlugin extends Plugin[Core]{
    // val debug = new Bundle{
    //     val wen = Bool
    //     val wnum = Bits(5 bits)
    //     val wdata = Bits(32 bits)
        
    // }
    // val debug = out(new DebugBundle())

    val wvalid = Bool
    val waddr = Bits(RegAddrWidth bits).addAttribute("DONT_TOUCH")
    val wdata = Bits(DataWidth bits).addAttribute("DONT_TOUCH")

    val fromEXE1 = Bits(DataWidth bits)
    val fromEXE2 = Bits(DataWidth bits)
    val fromWB = Bits(DataWidth bits)

    val src1Addr = UInt(RegAddrWidth bits)
    val src2Addr = UInt(RegAddrWidth bits)

    val rs1Forwardable = Bool
    val rs1ForwardType = Bits(ForwardType().getBitsWidth bits)
    val rs2Forwardable = Bool
    val rs2ForwardType = Bits(ForwardType().getBitsWidth bits)
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        val global = pipeline plug new Area{
            val regFile = Mem(Bits(32 bits), NR_REG).addAttribute(Verilator.public).addAttribute("DONT_TOUCH")
            regFile.init(List.fill(NR_REG)(B(0, 32 bits)))

            switch(EXE1.output(writeSignals.FUType_WB)) {
                is(FuType.ALU) {
                    fromEXE1 := EXE1.output(writeSignals.ALU_RESULT_WB)
                }
                is(FuType.BRU) {
                    fromEXE1 := (EXE1.output(fetchSignals.PC).asUInt + U(4)).asBits
                }
                default{
                    fromEXE1 := 0
                }
            }

            switch(EXE2.output(writeSignals.FUType_WB)) {
                is(FuType.ALU) {
                    fromEXE2 := EXE2.output(writeSignals.ALU_RESULT_WB)
                }
                is(FuType.BRU) {
                    fromEXE2 := (EXE2.output(fetchSignals.PC).asUInt + U(4)).asBits
                }
                is(FuType.LSU) {
                    fromEXE2 := EXE2.output(writeSignals.MEM_RDATA_WB)
                }
            }
            fromWB := wdata

            val rs1BypassNetwork = new BypassNetwork()
            val rs2BypassNetwork = new BypassNetwork()
            rs1BypassNetwork.io.rsISS <> src1Addr.asBits
            rs1BypassNetwork.io.rdEXE1 <> EXE1.output(writeSignals.REG_WRITE_ADDR_WB)
            rs1BypassNetwork.io.rdEXE2 <> EXE2.output(writeSignals.REG_WRITE_ADDR_WB)
            rs1BypassNetwork.io.rdWB   <> waddr
            rs1BypassNetwork.io.fuTypeEXE1 <> EXE1.output(writeSignals.FUType_WB)
            rs1BypassNetwork.io.regWriteValidEXE1 := EXE1.output(writeSignals.REG_WRITE_VALID_WB) & EXE1.arbitration.isValidNotStuck
            rs1BypassNetwork.io.regWriteValidEXE2 := EXE2.output(writeSignals.REG_WRITE_VALID_WB) & EXE2.arbitration.isValidNotStuck
            rs1BypassNetwork.io.regWriteValidWB   <> wvalid
            rs1BypassNetwork.io.rsForwardType     <> rs1ForwardType
            rs1BypassNetwork.io.forwardable       <> rs1Forwardable

            rs2BypassNetwork.io.rsISS <> src2Addr.asBits
            rs2BypassNetwork.io.rdEXE1 <> EXE1.output(writeSignals.REG_WRITE_ADDR_WB)
            rs2BypassNetwork.io.rdEXE2 <> EXE2.output(writeSignals.REG_WRITE_ADDR_WB)
            rs2BypassNetwork.io.rdWB   <> waddr
            rs2BypassNetwork.io.fuTypeEXE1 <> EXE1.output(writeSignals.FUType_WB)
            rs2BypassNetwork.io.regWriteValidEXE1 := EXE1.output(writeSignals.REG_WRITE_VALID_WB) & EXE1.arbitration.isValidNotStuck
            rs2BypassNetwork.io.regWriteValidEXE2 := EXE2.output(writeSignals.REG_WRITE_VALID_WB) & EXE2.arbitration.isValidNotStuck
            rs2BypassNetwork.io.regWriteValidWB   <> wvalid
            rs2BypassNetwork.io.rsForwardType     <> rs2ForwardType
            rs2BypassNetwork.io.forwardable       <> rs2Forwardable

        }

        ISS plug new Area{
            import ISS._

            val inst = input(fetchSignals.INST)
            val fuType = output(decodeSignals.FUType)
            val src1Addr = U(inst(LA32R.rjRange))
            val src2Addr = (fuType === FuType.ALU) ? U(inst(LA32R.rkRange)) | U(inst(LA32R.rdRange))

            // val src1Data = (wvalid && src1Addr.asBits === waddr && (input(decodeSignals.SRC1_FROM) === ALUOpSrc.REG)) ? (wdata) | global.regFile.readAsync(src1Addr)
            // val src2Data = (wvalid && src2Addr.asBits === waddr && (input(decodeSignals.SRC2_FROM) === ALUOpSrc.REG)) ? (wdata) | global.regFile.readAsync(src2Addr)
            val src1Data = Bits(32 bits)
            val src2Data = Bits(32 bits)
            switch(rs1ForwardType){
                is(ForwardType.FROMEXE1.asBits) {
                    src1Data := fromEXE1
                }
                is(ForwardType.FROMEXE2.asBits) {
                    src1Data := fromEXE2
                }
                is(ForwardType.FROMWB.asBits) {
                    src1Data := wdata
                }
                default {
                    src1Data := global.regFile.readAsync(src1Addr)
                }
            }

            switch(rs2ForwardType){
                is(ForwardType.FROMEXE1.asBits) {
                    src2Data := fromEXE1
                }
                is(ForwardType.FROMEXE2.asBits) {
                    src2Data := fromEXE2
                }
                is(ForwardType.FROMWB.asBits) {
                    src2Data := wdata
                }
                default {
                    src2Data := global.regFile.readAsync(src2Addr)
                }
            }
            
            insert(decodeSignals.SRC1Addr) := src1Addr.asBits
            insert(decodeSignals.SRC2Addr) := src2Addr.asBits
            insert(decodeSignals.SRC1) := (src1Addr === 0) ? B(0, 32 bits) | src1Data
            insert(decodeSignals.SRC2) := (src2Addr === 0) ? B(0, 32 bits) | src2Data
            insert(decodeSignals.REG_WRITE_ADDR) := (input(decodeSignals.JUMPType) =/= JumpType.JBL) ? inst(LA32R.rdRange) | B"5'h1"
            
        }

        WB plug new Area{
            import WB._

            val regWritePort = global.regFile.writePort()

            wvalid := input(decodeSignals.REG_WRITE_VALID) && arbitration.isValidNotStuck
            waddr := input(writeSignals.REG_WRITE_ADDR_WB)
            // val aluResult = input(RESULT)
            // val memRdata = input(MEM_RDATA)
            // val data = input(REG_WRITE_DATA)
            // val data = Select{
            //     (input(FUType) === ALU) -> input(RESULT);
            //     ((input(FUType) === LSU) && (input(MEM_READ) =/= B"0000")) -> input(MEM_RDATA);
            //     ((input(FUType) === BRU) && ((input(JUMPType) === JBL) || (input(JUMPType) === JB))) -> (input(PC).asUInt + U(4)).asBits;
            //     default -> B"32'h00000000"
            // }

            switch(input(writeSignals.FUType)){
                is(ALU){
                    wdata := input(writeSignals.ALU_RESULT_WB)
                }
                is(LSU){
                    wdata := input(writeSignals.MEM_RDATA_WB)
                }
                is(BRU){
                    wdata := (input(fetchSignals.PC).asUInt + U(4)).asBits
                }
            }

            // debug.pc := input(fetchSignals.PC)
            // debug.we := wvalid
            // debug.wnum := waddr
            // debug.wdata := wdata

            regWritePort.valid := wvalid
            regWritePort.address := U(waddr)
            regWritePort.data := wdata
        }
    }
  
}
