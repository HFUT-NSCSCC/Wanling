package myCPU.constants

import spinal.core.SpinalEnum
import spinal.core.binaryOneHot
import spinal.core.binarySequential
import spinal.core.SpinalEnumElement


// 前推类型
object ForwardType extends SpinalEnum(binarySequential){
    val FROMEXE1, FROMEXE2, FROMEXE3, FROMWB = newElement()
    val FROMREG = newElement()
  
}
