package myCPU.pipeline.decode

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import spinal.core.internals.Literal
import spinal.lib._
import spinal.lib.logic.Masked
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.math.BigInt
import myCPU.builder.Stageable
import _root_.spinal.lib.logic.Symplify

class DecoderPlugin extends Plugin[Core]{
    val encodings = mutable.LinkedHashMap[MaskedLiteral, ArrayBuffer[(Stageable[_ <: BaseType], BaseType)]]()
    val defaults = mutable.LinkedHashMap[Stageable[_ <: BaseType], BaseType]()
    
    def add(key: MaskedLiteral, values: Seq[(Stageable[_ <: BaseType], Any)]): Unit = {
        val instructionModel = encodings.getOrElseUpdate(key, ArrayBuffer[(Stageable[_ <: BaseType], BaseType)]())
        values.map{case (a, b) => {
            assert(!instructionModel.contains(a), s"Over specification of $a")
            val value = b match{
                case e: SpinalEnumElement[_] => e()
                case e: BaseType => e
            }
            instructionModel += (a -> value)
        }}
    }

    def add(encoding: Seq[(MaskedLiteral, Seq[(Stageable[_ <: BaseType], Any)])]): Unit = {
        encoding.foreach(e => this.add(e._1, e._2))
    }

    def addDefault(key: Stageable[_ <: BaseType], value: Any): Unit = {
        assert(!defaults.contains(key))
        defaults(key) = value match {
            case e: SpinalEnumElement[_] => e()
            case e: BaseType => e
        }
    }

    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline.config._
        import pipeline.ID._

        val stageables = (encodings.flatMap(_._2.map(_._1)) ++ defaults.map(_._1)).toList.distinct
        var offset = 0
        var defaultValue, defaultCare = BigInt(0)
        val offsetOf = mutable.LinkedHashMap[Stageable[_ <: BaseType], Int]()

        stageables.foreach(e => {
            defaults.get(e) match {
                case Some(value) => {
                    value.head.source match {
                        case literal: EnumLiteral[_] => literal.fixEncoding(e.dataType.asInstanceOf[SpinalEnumCraft[_]].getEncoding)
                        case _ => 
                    }
                    defaultValue += value.head.source.asInstanceOf[Literal].getValue << offset
                    defaultCare += ((BigInt(1) << e.dataType.getBitsWidth) - 1) << offset
                }
                case _ => 
            }
            offsetOf(e) = offset
            offset += e.dataType.getBitsWidth
        })

        val spec = encodings.map{case(key, values) => 
            var decodedValue = defaultValue    
            var decodedCare = defaultCare
            for ((e, literal) <- values) {
                literal.head.source match {
                    case literal: EnumLiteral[_] => literal.fixEncoding(e.dataType.asInstanceOf[SpinalEnumCraft[_]].getEncoding)
                    case _ =>
                }
                val offset = offsetOf(e)
                decodedValue |= literal.head.source.asInstanceOf[Literal].getValue << offset
                decodedCare |= ((BigInt(1) << e.dataType.getBitsWidth) - 1) << offset
            }
            (Masked(key.value, key.careAbout), Masked(decodedValue, decodedCare))
        }

        val decodedBits = Bits(stageables.foldLeft(0)(_ + _.dataType.getBitsWidth) bits)
        decodedBits := Symplify(input(INST), spec, decodedBits.getWidth)

        offset = 0
        stageables.foreach(e => {
            insert(e).assignFromBits(decodedBits(offset, e.dataType.getBitsWidth bits))
            offset += e.dataType.getBitsWidth
        })

    }
        
}
