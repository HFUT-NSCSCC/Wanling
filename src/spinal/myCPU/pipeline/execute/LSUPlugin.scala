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
            val we = lsuSignals.MEM_WRITE & (arbitration.isValidNotStuck.asSInt(4 bits).asBits)
            data.we := we
            data.addr := vaddr.asBits
            // insert(MEM_RDATA) := data.rdata
            // val wdata = RegNextWhen[Bits](lsuSignals.SRC2, we.asBool, init = 0)
            data.wdata := lsuSignals.SRC2
            val rdata = data.rdata
            // insert(writeSignals.MEM_RDATA) := lsuSignals.MEM_READ_UE ? rdata.asUInt.resize(32 bits).asBits | rdata.asSInt.resize(32 bits).asBits
            insert(writeSignals.MEM_RDATA) := rdata
        }
        
        EXE2 plug new Area{
            import EXE2._

            // val lsuSignals = input(exeSignals.lsuSignals)
            // val rdata = RegNextWhen[Bits](data.rdata, !arbitration.isStuck, init = 0)
            // // TODO: 这似乎并不能实现真正的扩展，读进来的rdata都是32位的
            // insert(writeSignals.MEM_RDATA) := lsuSignals.MEM_READ_UE ? rdata.asUInt.resize(32 bits).asBits | rdata.asSInt.resize(32 bits).asBits


        }
    }
}
