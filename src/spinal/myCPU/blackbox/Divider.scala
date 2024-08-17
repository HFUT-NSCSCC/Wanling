package myCPU.blackbox
import spinal.core._
import spinal.lib._

class Divider(dataWidth: Int = 32, name: String = "divider") extends BlackBox{
  setDefinitionName(name)
  noIoPrefix()
  val io = new Bundle{
    val aclk = in Bool()
    val s_axis_divisor_tvalid = in Bool()
    val s_axis_divisor_tdata = in Bits(dataWidth bits)
    val s_axis_dividend_tvalid = in Bool()
    val s_axis_dividend_tdata = in Bits(dataWidth bits)
    val m_axis_dout_tvalid = out Bool()
    val m_axis_dout_tdata = out Bits(2 * dataWidth bits)
  }
  mapClockDomain(clock = io.aclk)
}
