package edu.umontreal.kotlingrad.api

import edu.umontreal.kotlingrad.api.VFun.Companion.KG_IT

// Supports arbitrary subgraph reassignment but usually just holds variable-to-value bindings
@Suppress("UNCHECKED_CAST")
data class Bindings<X: SFun<X>> constructor(val fMap: Map<Fun<X>, Fun<X>>) {
  constructor(inputs: List<Bindings<X>>): this(inputs.map { it.fMap }
    .fold(mapOf<Fun<X>, Fun<X>>()) { acc, fMap -> fMap + acc })
  constructor(vararg bindings: Bindings<X>): this(bindings.toList())
  constructor(vararg funs: Fun<X>): this(funs.map { it.bindings })
  constructor(vararg pairs: Pair<Fun<X>, Fun<X>>): this(pairs.toMap())

  // TODO: Take shape into consideration when binding variables
  fun zip(fns: List<Fun<X>>): Bindings<X> =
    (sVars.zip(fns.filterIsInstance<SFun<X>>()) +
      vVars.zip(fns.filterIsInstance<VFun<X, *>>()) +
      mVars.zip(fns.filterIsInstance<MFun<X, *, *>>()))
      .let { Bindings(*it.toTypedArray()) }

  // Scalar, vector, and matrix "views" on untyped function map
  val sFunMap = filterInstancesOf<SFun<X>>()
  val vFunMap = filterInstancesOf<VFun<X, *>>()
  val mFunMap = filterInstancesOf<MFun<X, *, *>>()

  val mVarMap = mFunMap.filterKeys { it is MVar<X, *, *> } as Map<MVar<X, *, *>, MFun<X, *, *>>
  val vVarMap = mVarMap.filterValues { it is Mat<X, *, *> }
    .flatMap { (k, v) -> k.vVars.zip((v as Mat<X, *, *>).rows) }.toMap() +
    vFunMap.filterKeys { it is VVar<X, *> } as Map<VVar<X, *>, VFun<X, *>>
  val sVarMap = (vVarMap.filterValues { it is Vec<X, *> }
    .flatMap { (k, v) -> k.sVars.contents.zip((v as Vec<X, *>).contents) }.toMap() +
    sFunMap.filterKeys { it is SVar<X> && it.name != KG_IT }) as Map<SVar<X>, SFun<X>>

  val allVarMap = mVarMap + vVarMap + sVarMap

  private inline fun <reified T> filterInstancesOf(): Map<T, T> =
    fMap.filterKeys { it is T } as Map<T, T>

  // Merges two variable bindings
  // TODO: Add support for change of variables, i.e. x = y, y = 2z, z = x + y...
  operator fun plus(other: Bindings<X>) =
    Bindings(fMap + other.fMap +
      allVarMap.filterValues { containsFreeVariable(it) } +
      other.allVarMap.filterValues { containsFreeVariable(it) } +
      allVarMap.filterValues { !containsFreeVariable(it) } +
      other.allVarMap.filterValues { !containsFreeVariable(it) }
    )

  operator fun plus(pair: Pair<Fun<X>, Fun<X>>) = plus(Bindings(pair))

  operator fun minus(func: Fun<X>) = Bindings(fMap.filterNot { it.key == func })

  // Scalar, vector, and matrix variables
  val sVars: Set<SVar<X>> = sVarMap.keys.toSortedSet { v1, v2 -> v1.name.compareTo(v2.name) }
  val vVars: Set<VVar<X, *>> = vVarMap.keys.toSortedSet { v1, v2 -> v1.name.compareTo(v2.name) }
  val mVars: Set<MVar<X, *, *>> = mVarMap.keys.toSortedSet { v1, v2 -> v1.name.compareTo(v2.name) }
  val allVars: Set<Variable<X>> = sVars + vVars + mVars
  val allFreeVariables by lazy { allVarMap.filterValues { containsFreeVariable(it) } }
  val allBoundVariables: Map<Variable<X>, Fun<X>> by lazy { allVarMap.filterValues { !containsFreeVariable(it) } }

  private fun containsFreeVariable(it: Fun<X>): Boolean =
    (it is Mat<X, *, *> && it.bindings.allFreeVariables.isNotEmpty()) ||
      (it is MFun<X, *, *> && it !is Mat<X, *, *> && it !is MConst<X, *, *>) ||
      (it is Vec<X, *> && it.bindings.allFreeVariables.isNotEmpty()) ||
      (it is VFun<X, *> && it !is Vec<X, *> && it !is VConst<X, *>) ||
      (it is SFun<X> && it !is Constant)

  val complete = allFreeVariables.isEmpty()
  val readyToBind = allBoundVariables.isNotEmpty()

  fun fullyDetermines(fn: SFun<X>) = fn.bindings.allVars.all { it in this }
  operator fun contains(v: Fun<X>) = v in allVars
  fun curried() = fMap.map { (k, v) -> Bindings(k to v) }

  // Intended for debugging purposes, should be removed eventually
  fun checkForUnpropagatedVariables(before: Fun<X>, after: Fun<X>) {
    val freeVars = after.bindings.allFreeVariables.keys
    val boundVars = allBoundVariables.keys
    val unpropagated = (freeVars intersect boundVars).map { it to this[it] }
    require(unpropagated.isEmpty()) {
      before.show("input"); after.show("result")
      "Free vars: $freeVars\n" +
        "Bindings were $this\n" +
        "Result included unpropagated variables: $unpropagated"
    }
  }

  operator fun <T: Fun<X>> get(t: T): T? = (allVarMap[t as? Variable<X>] ?: fMap[t]) as? T?
  override fun equals(other: Any?) = other is Bindings<*> && fMap == other.fMap
  override fun hashCode() = fMap.hashCode()
  override fun toString() = fMap.toString()
}
