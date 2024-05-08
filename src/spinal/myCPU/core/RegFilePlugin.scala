package myCPU.core

import myCPU.builder.Plugin
import spinal.core._
import spinal.lib._

class RegFilePlugin extends Plugin[Core]{
    val hello = RegNext(True)
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        val NR_REG = 32
        val global = pipeline plug new Area{
            val regFile = Mem(Bits(32 bits), NR_REG).addAttribute(Verilator.public)
            regFile.init(List.fill(NR_REG)(B(0, 32 bits)))
        }

        ID plug new Area{
            import ID._

            val inst = input(INST)
            val rdAddr = U(inst(LA32R.rdRange))
            val rjAddr = U(inst(LA32R.rjRange))
            val rkAddr = U(inst(LA32R.rkRange))
            val raAddr = U(inst(LA32R.raRange))
            val rdData = (rdAddr === 0) ? global.regFile.readAsync(rdAddr) | B(0, 32 bits)
            val rjData = (rjAddr === 0) ? global.regFile.readAsync(rjAddr) | B(0, 32 bits)
            val rkData = (rkAddr === 0) ? global.regFile.readAsync(rkAddr) | B(0, 32 bits)
            val raData = (raAddr === 0) ? global.regFile.readAsync(raAddr) | B(0, 32 bits)
            
            insert(RD) := rdData
            insert(RJ) := rjData
            insert(RK) := rkData
            insert(RA) := raData
        }

        WB plug new Area{
            import WB._

            val regWritePort = global.regFile.writePort()

            regWritePort.valid := input(REG_WRITE_VALID)
            regWritePort.address := U(input(REG_WRITE_ADDR))(4 downto 0)
            regWritePort.data := input(REG_WRITE_DATA)
        }

        
    }
  
}
