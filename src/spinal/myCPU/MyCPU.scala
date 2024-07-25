package myCPU
import spinal.core._
import spinal.lib._
import myCPU.core._
import myCPU.pipeline.fetch._
import myCPU.pipeline.decode._
import myCPU.pipeline.execute._
import myCPU.peripheral.Bridge
import myCPU.peripheral.BusBundle
import _root_.myCPU.peripheral.Sram
import myCPU.peripheral.ExtSramCtrl
import myCPU.peripheral.BaseSramCtrl

case class InstBundle() extends Bundle with IMasterSlave{
    val en = (Bool)
    // val we = (Bits(4 bits))
    val addr = (Bits(32 bits))
    val rdata = (Bits(32 bits))
    
    def asMaster(): Unit = {
        out(en, addr)
        in(rdata)
    }
}

case class DataBundle() extends Bundle with IMasterSlave{
    val en = (Bool)
    val we = (Bits(4 bits))
    val addr = (Bits(32 bits))
    val wdata = (Bits(32 bits))
    val rdata = (Bits(32 bits))
    // val do_store = (Bool)
    val do_store_base = (Bool)
    val do_store_ext  = (Bool)

    def asMaster(): Unit = {
        out(en, we, addr, wdata)
        in(rdata, do_store_base, do_store_ext)
    }
}

class DebugBundle() extends Bundle{
    val pc = Bits(32 bits)
    val we = Bool
    val wnum = Bits(5 bits)
    val wdata = Bits(32 bits)
}

class MyCPU(val config: CoreConfig) extends Component{
    val io = new Bundle{
        val clk = in(Bool)
        val reset = in(Bool)
        // val inst = master(InstBundle())
        // val data = master(DataBundle())
        // val instSram = master(BusBundle("inst_sram"))
        // val dataSram = master(BusBundle("data_sram"))
        val baseSram = master(Sram("base_ram"))
        val extSram = master(Sram("ext_ram"))
        val conf = master(BusBundle("conf"))
    }

    val defaultClockDomain = ClockDomain(
        clock = io.clk,
        reset = io.reset,
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
        // io.debug <> regFile.debug

        val fetcherPlugin = cpu.service(classOf[FetcherPlugin])
        val baseSramCtrl = new BaseSramCtrl()
        baseSramCtrl.io.instBundle <> fetcherPlugin.instBundle
        baseSramCtrl.io.baseSram <> io.baseSram

        val LSUPlugin = cpu.service(classOf[LSUPlugin])
        val bridge = new Bridge()
        val extSramCtrl = new ExtSramCtrl()
        bridge.io.dBus <> LSUPlugin.data
        bridge.io.instSram <> baseSramCtrl.io.instSram

        bridge.io.dataSram  <> extSramCtrl.io.dataSram
        extSramCtrl.io.extSram <> io.extSram
        bridge.io.conf <> io.conf
    }
  
}
