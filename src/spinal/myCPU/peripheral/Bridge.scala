package myCPU.peripheral
import spinal.core._
import spinal.lib.IMasterSlave
import myCPU.DataBundle
import spinal.lib._

case class BusBundle(targetName: String) extends Bundle with IMasterSlave{
    setName(targetName)
    val en = Bool
    val we = Bits(4 bits)
    val addr = Bits(32 bits)
    val wdata = Bits(32 bits)
    val rdata = Bits(32 bits)
    val do_store = Bool

    def asMaster(): Unit = {
        out(en, we, addr, wdata)
        in(rdata, do_store)
    }
}

object BridgeConf {
    def ADDR_BASE = B"32'x0000_0000"
    def ADDR_EXT = B"32'x0040_0000"
    def ADDR_MASK = B"32'x7fc0_0000"
}

class Bridge extends Component{
    val io = new Bundle{
        val dBus = slave(DataBundle())
        val instSram = master(BusBundle("inst_sram"))
        val dataSram = master(BusBundle("data_sram"))
        val conf = master(BusBundle("conf"))
    }

    val sel_base = (io.dBus.addr & BridgeConf.ADDR_MASK) === BridgeConf.ADDR_BASE
    val sel_ext = (io.dBus.addr & BridgeConf.ADDR_MASK) === BridgeConf.ADDR_EXT
    val sel_conf = !sel_base && !sel_ext

    val sel_base_r = RegNext(sel_base, init = False)
    val sel_ext_r  = RegNext(sel_ext, init = False)
    val sel_conf_r = RegNext(sel_conf, init = False)

    io.instSram.en := io.dBus.en & sel_base
    io.instSram.we := (io.instSram.en #* 4) & io.dBus.we
    io.instSram.addr := io.dBus.addr
    io.instSram.wdata := io.dBus.wdata

    io.dataSram.en := io.dBus.en & sel_ext
    io.dataSram.we := (io.dataSram.en #* 4) & io.dBus.we
    io.dataSram.addr := io.dBus.addr
    io.dataSram.wdata := io.dBus.wdata

    io.conf.en := io.dBus.en & sel_conf
    io.conf.we := (io.conf.en #* 4) & io.dBus.we
    io.conf.addr := io.dBus.addr
    io.conf.wdata := io.dBus.wdata

    io.dBus.rdata := ((sel_base_r #* 32) & io.instSram.rdata) |
                     ((sel_ext_r  #* 32) & io.dataSram.rdata) |
                     ((sel_conf_r #* 32) & io.conf.rdata)

    io.dBus.do_store_base := io.instSram.do_store
    io.dBus.do_store_ext := io.dataSram.do_store
}
