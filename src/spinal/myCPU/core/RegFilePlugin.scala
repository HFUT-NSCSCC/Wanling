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
    val hello = RegNext(True)

    // val debug = new Bundle{
    //     val wen = Bool
    //     val wnum = Bits(5 bits)
    //     val wdata = Bits(32 bits)
        
    // }
    val debug = out(new DebugBundle())

    val wvalid = Bool
    val waddr = Bits(RegAddrWidth bits)
    val wdata = Bits(DataWidth bits)
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        val global = pipeline plug new Area{
            val regFile = Mem(Bits(32 bits), NR_REG).addAttribute(Verilator.public)
            regFile.init(List.fill(NR_REG)(B(0, 32 bits)))
        }

        ID plug new Area{
            import ID._

            val inst = input(fetchSignals.INST)
            val fuType = output(decodeSignals.FUType)
            val src1Addr = U(inst(LA32R.rjRange))
            val src2Addr = (fuType === FuType.ALU) ? U(inst(LA32R.rkRange)) | U(inst(LA32R.rdRange))

            val src1Data = (src1Addr =/= 0) ? global.regFile.readAsync(src1Addr) | B(0, 32 bits)
            val src2Data = (src2Addr =/= 0) ? global.regFile.readAsync(src2Addr) | B(0, 32 bits)
            
            insert(decodeSignals.SRC1Addr) := src1Addr.asBits
            insert(decodeSignals.SRC2Addr) := src2Addr.asBits
            insert(decodeSignals.SRC1) := (wvalid && src1Addr.asBits === waddr) ? (wdata) | src1Data
            insert(decodeSignals.SRC2) := (wvalid && src2Addr.asBits === waddr) ? (wdata) | src2Data
            
        }

        WB plug new Area{
            import WB._

            val regWritePort = global.regFile.writePort()

            wvalid := input(decodeSignals.REG_WRITE_VALID) && arbitration.notStuck
            waddr := (input(writeSignals.JUMPType) =/= JBL) ? input(fetchSignals.INST)(4 downto 0) | B"5'h1"
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
                    wdata := input(writeSignals.ALU_RESLUT)
                }
                is(LSU){
                    wdata := input(writeSignals.MEM_RDATA)
                }
                is(BRU){
                    wdata := (input(fetchSignals.PC).asUInt + U(4)).asBits
                }
            }

            debug.pc := input(fetchSignals.PC)
            debug.we := wvalid
            debug.wnum := waddr
            debug.wdata := wdata

            regWritePort.valid := wvalid
            regWritePort.address := U(waddr)
            regWritePort.data := wdata
        }
    }
  
}
