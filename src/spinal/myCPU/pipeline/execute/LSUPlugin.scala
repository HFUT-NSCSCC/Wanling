package myCPU.pipeline.execute

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import spinal.lib._
import myCPU.DataBundle

class LSUPlugin extends Plugin[Core]{
    val data = new DataBundle()
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        EXE1 plug new Area{
            import EXE1._

            val lsuSignals = input(exeSignals.lsuSignals)

            val src1 = lsuSignals.SRC1.asUInt //rj
            val imm = lsuSignals.IMM.asUInt // imm
            val vaddr = src1 + imm
            data.en := (lsuSignals.MEM_READ =/= B"0000" || lsuSignals.MEM_WRITE =/= B"0000")
            data.we := lsuSignals.MEM_WRITE
            data.addr := vaddr.asBits
            // insert(MEM_RDATA) := data.rdata
            data.wdata := lsuSignals.SRC2
        }
        
        EXE2 plug new Area{
            import EXE2._

            val lsuSignals = input(exeSignals.lsuSignals)

            val rdata = data.rdata
            insert(writeSignals.MEM_RDATA) := lsuSignals.MEM_READ_UE ? rdata.asUInt.resize(32 bits).asBits | rdata.asSInt.resize(32 bits).asBits
        }
    }
}
