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

            val inst = input(INST)
            val fuType = output(FUType)
            val src1Addr = U(inst(LA32R.rjRange))
            val src2Addr = (fuType === FuType.ALU) ? U(inst(LA32R.rkRange)) | U(inst(LA32R.rdRange))

            val src1Data = (src1Addr === 0) ? global.regFile.readAsync(src1Addr) | B(0, 32 bits)
            val src2Data = (src2Addr === 0) ? global.regFile.readAsync(src2Addr) | B(0, 32 bits)
            
            insert(SRC1Addr) := src1Addr.asBits
            insert(SRC2Addr) := src2Addr.asBits
            insert(SRC1) := src1Data
            insert(SRC2) := src2Data
        }

        WB plug new Area{
            import WB._

            val regWritePort = global.regFile.writePort()

            val valid = input(REG_WRITE_VALID)
            val address = (input(JUMPType) =/= JBL) ? input(INST)(4 downto 0) | B"5'h1"
            val aluResult = input(RESULT)
            val memRdata = input(MEM_RDATA)
            // val data = input(REG_WRITE_DATA)
            val data = Select{
                (input(FUType) === ALU) -> aluResult;
                ((input(FUType) === LSU) && (input(MEM_READ) =/= B"0000")) -> memRdata;
                ((input(FUType) === BRU) && ((input(JUMPType) === JBL) || (input(JUMPType) === JB))) -> (input(PC).asUInt + U(4)).asBits;
                default -> B"32'h00000000"
            }

            debug.pc := input(PC)
            debug.we := valid
            debug.wnum := address
            debug.wdata := data

            regWritePort.valid := valid
            regWritePort.address := U(address)
            regWritePort.data := data
        }
    }
  
}
