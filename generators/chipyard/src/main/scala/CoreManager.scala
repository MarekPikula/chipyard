package chipyard

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

import chisel3._

import freechips.rocketchip.config.{Parameters, Config, Field, View}
import freechips.rocketchip.subsystem.{SystemBusKey, RocketTilesKey, RocketCrossingParams}
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.diplomaticobjectmodel.logicaltree.LogicalTreeNode
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._

import ariane.{ArianeTile, ArianeTilesKey, ArianeCrossingKey, ArianeTileParams}

// Third-party core entries
sealed trait CoreEntryBase {
  def updateWithFilter(view: View, p: Any => View): (Map[String, Any] => PartialFunction[Any, Seq[AnyRef]])

  def instantiateTile(param: TileParams, crossing: RocketCrossingParams,
    logicalTreeNode: LogicalTreeNode, p: Parameters): Option[BaseTile]
}

class CoreEntry[TileParamsT <: CoreParams, TileT <: BaseTile](
  tk: Field[Seq[TileParamsT]],
  ck: Field[Seq[RocketCrossingParams]]
) extends CoreEntryBase {
  private val mirror = runtimeMirror(getClass.getClassLoader)
  private val paramClass = mirror.runtimeClass(typeOf[TileParamsT].typeSymbol.asClass)
  private val paramNames = Map((paramClass.getDeclaredFields map _.getName).zipWithIndex)
  private val paramCtr = paramClass.getConstructors.head

  private val tileClass = mirror.runtimeClass(typeOf[TileT].typeSymbol.asClass)
  private val tileCtr = paramClass.getConstructors.head

  // copy() function in
  def copyTileParam(tileParam: AnyRef, properties: Map[String, Any]) = {
    val values = foo.productIterator.toList
    val indexedProperties = properties map (key => (paramNames(key), properties(key)))
    val newValues = (0 until values.size) map
      (i => if (indexedProperties contains i) indexedProperties(i) else values(i))
    paramCtr.newInstance(newValues:_*)
  }

  def updateWithFilter(view: View, p: Any => View) = {
    case key if (key == tk && p(tk)) => view(tk) map
      (tile => properties => copyTileParam(tile, properties))
  }

  def instantiateTile(param: TileParams, crossing: RocketCrossingParams,
    logicalTreeNode: LogicalTreeNode, p: Parameters): Option[BaseTile] = param match {
    case a: TileParams => Some(tileCtr.newInstance(a, crossing, PriorityMuxHartIdFromSeq(p(tilesKey)), logicalTreeNode, p))
    case _ => None
  }
}

object CoreManager {
  val cores: List[CoreEntryBase] = List(
    // ADD YOUR CORE DEFINITION HERE
    new CoreEntry[ArianeTileParams, ArianeTile](ArianeTilesKey, ArianeCrossingKey)
  )
}

// Core Generic Config - change properties in the given map
class GenericConfig(properties: Map[String, Any], filterFunc: Any => Bool) {
  val configFunc: (View, View, View) => PartialFunction[Any, Any] = ((site, here, up) => key => {
    val tiles = CoreManager.cores flatMap _.updateWithFilter(up, filterFunc).lift(key)
    if (tiles.size == 0) None else Some(tiles map _(properties))
  }).unlift
}

object GenericConfig {
  def apply(properties: Map[String, Any], filterFunc: Any => Bool = (_ => true)) =
    new GenericConfig(properties, filterFunc).configFunc
}
