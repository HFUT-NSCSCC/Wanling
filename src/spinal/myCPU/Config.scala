package myCPU

import spinal.core._
import spinal.core.sim._
import spinal.lib.bus.tilelink.coherent.Cache

object Config {
  def spinal = SpinalConfig(
    targetDirectory = "build/gen",
    onlyStdLogicVectorAtTopLevelIo = true,
  )

  def sim = SimConfig.withConfig(spinal).withFstWave
}

abstract class CacheBasicConfig {
  val sets: Int
  val lineSize: Int
  val ways: Int
  val offsetWidth = log2Up(lineSize)
  val offsetRange = (offsetWidth - 1) downto 0
  val wordOffsetRange = (offsetWidth - 1) downto 2
  val indexWidth = log2Up(sets)
  val indexRange = (offsetWidth + indexWidth - 1) downto offsetWidth
  val tagOffset = offsetWidth + indexWidth
  val tagRange = 31 downto tagOffset
  def wordCount = sets * lineSize / 4
  def lineWords = lineSize / 4
}

final case class ICacheConfig(
  sets: Int = 64,
  lineSize: Int = 64,
  ways: Int = 2,
) extends CacheBasicConfig {
  val enable = true
}