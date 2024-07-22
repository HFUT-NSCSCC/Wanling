package myCPU.constants

import spinal.core.SpinalEnum
import spinal.core.binaryOneHot
import spinal.core.binarySequential
import spinal.core.SpinalEnumElement

object ForwardType extends SpinalEnum(binarySequential){
    val FROMEXE1, FROMEXE2, FROMWB = newElement()
    val FROMREG = newElement()
  
}
