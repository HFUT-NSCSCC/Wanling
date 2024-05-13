package myCPU.constants

import spinal.core.SpinalEnum
import spinal.core.binarySequential

object BRUOpType extends SpinalEnum(binarySequential){
    val NONE = newElement()
    val EQ, NEQ = newElement()
    val LT, LTU = newElement()
    val GE, GEU = newElement()
}
