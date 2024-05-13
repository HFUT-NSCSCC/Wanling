package myCPU.constants

import spinal.core.UInt
import spinal.core.log2Up

object LA32 {
    def PCWidth = 32
    def PC_INIT:Long = 0x1c000000L

    def InstWidth = 32

    def NR_REG = 32
    def RegAddrWidth = log2Up(NR_REG)

    def DataWidth = 32
    def AddrWidth = 32
}
