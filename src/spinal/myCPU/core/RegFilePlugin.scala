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
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        val global = pipeline plug new Area{
            val regFile = Mem(Bits(32 bits), NR_REG).addAttribute(Verilator.public).addAttribute("DONT_TOUCH")
            regFile.init(List.fill(NR_REG)(B(0, 32 bits)))
        }

        ISS plug new Area{
            import ISS._

            val inst = input(fetchSignals.INST)
            val fuType = input(decodeSignals.FUType)
            val src1Addr = U(inst(LA32R.rjRange))
            val src2Addr = (fuType === FuType.ALU) ? U(inst(LA32R.rkRange)) | U(inst(LA32R.rdRange))

            val src1Data = (wvalid && src1Addr.asBits === waddr && (input(decodeSignals.SRC1_FROM) === ALUOpSrc.REG)) ? (wdata) | global.regFile.readAsync(src1Addr)
            val src2Data = (wvalid && src2Addr.asBits === waddr && (input(decodeSignals.SRC2_FROM) === ALUOpSrc.REG)) ? (wdata) | global.regFile.readAsync(src2Addr)
            
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
            waddr := input(decodeSignals.REG_WRITE_ADDR)
            // val aluResult = input(RESULT)
            // val memRdata = input(MEM_RDATA)
            // val data = input(REG_WRITE_DATA)
            // val data = Select{
            //     (input(FUType) === ALU) -> input(RESULT);
            //     ((input(FUType) === LSU) && (input(MEM_READ) =/= B"0000")) -> input(MEM_RDATA);
            //     ((input(FUType) === BRU) && ((input(JUMPType) === JBL) || (input(JUMPType) === JB))) -> (input(PC).asUInt + U(4)).asBits;
            //     default -> B"32'h00000000"
            // }

            switch(input(writeSignals.FUTypeWB)){
                is(ALU){
                    wdata := input(writeSignals.ALU_RESULT)
                }
                is(LSU){
                    wdata := input(writeSignals.MEM_RDATA)
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
