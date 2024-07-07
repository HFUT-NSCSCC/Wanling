package myCPU.constants

import spinal.core.Spinal
import spinal.core.SpinalEnum
import spinal.core.binaryOneHot
import spinal.core.binarySequential

object ALUOpSrc extends SpinalEnum(binarySequential){
    val NONE = newElement()
    val REG = newElement()
    val IMM = newElement()
    val PC = newElement()
}
