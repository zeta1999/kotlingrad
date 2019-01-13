package edu.umontreal.kotlingrad.numerical

import ch.obermuhlner.math.big.BigDecimalMath.*
import edu.umontreal.kotlingrad.algebra.RealPrototype
import java.math.MathContext

object ProtoBigDecimal: RealPrototype<BigDecimalReal> {
  override val zero = BigDecimalReal(0.0)
  override val one = BigDecimalReal(1.0)
  val mc = MathContext(10)

  override fun cos(x: BigDecimalReal) = BigDecimalReal(cos(x.bg, mc))

  override fun sin(x: BigDecimalReal) = BigDecimalReal(sin(x.bg, mc))

  override fun tan(x: BigDecimalReal) = BigDecimalReal(tan(x.bg, mc))

  override fun exp(x: BigDecimalReal) = BigDecimalReal(exp(x.bg, mc))

  override fun log(x: BigDecimalReal) = BigDecimalReal(log(x.bg, mc))

  override fun pow(x: BigDecimalReal, y: BigDecimalReal) = BigDecimalReal(pow(x.bg, y.bg, mc))

  override fun sqrt(x: BigDecimalReal) = BigDecimalReal(sqrt(x.bg, mc))
}