package myCPU.constants

import spinal.core.SpinalEnum
import spinal.core.binarySequential

// 立即数扩展类型
object ImmExtType extends SpinalEnum(binarySequential){
    val NONE = newElement()
    val SI12, UI12 = newElement()
    val UI8 = newElement()
    val SI16 = newElement()
    val SI20 = newElement()
    val SI21 = newElement()
    val SI26 = newElement()
}
