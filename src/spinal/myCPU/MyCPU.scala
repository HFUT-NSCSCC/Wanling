package myCPU
import spinal.core._
import spinal.lib._
import myCPU.core._

class InstBundle() extends Bundle{
    val en = out(Bool)
    val addr = out(Bits(32 bits))
    val rdata = in(Bits(32 bits))
}

class DataBundle() extends Bundle{
    val en = out(Bool)
    val wen = out(Bool)
    val addr = out(Bits(32 bits))
    val wdata = out(Bits(32 bits))
    val rdata = in(Bits(32 bits))
}

class DebugBundle() extends Bundle{
    val pc = Bits(32 bits)
    val wen = Bool
    val wnum = Bits(5 bits)
    val wdata = Bits(32 bits)
}

class MyCPU(val config: CoreConfig) extends Component{
    val io = new Bundle{
        val clk = in(Bool)
        val resetn = in(Bool)
        val inst = new InstBundle()
        val data = new DataBundle()
        val debug = out(new DebugBundle())
    }

    val defaultClockDomain = ClockDomain(
        clock = io.clk,
        reset = io.resetn,
        config = ClockDomainConfig(
            resetKind = SYNC, 
            resetActiveLevel = LOW
        )
    )

    val defaultClockArea = new ClockingArea(defaultClockDomain){
        val cpu = new Core(config)

        val regFile = cpu.service(classOf[RegFilePlugin])
        // io.debug.pc <> 0x80000000 
        // io.debug.wen := regFile.debug.wen
        // io.debug.wnum := regFile.debug.wnum
        // io.debug.wdata := regFile.debug.wdata
        io.debug <> regFile.debug
    }
  
}
