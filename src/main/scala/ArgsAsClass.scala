package org.kr.args

import java.lang.reflect.Field
import java.time.{LocalDate, LocalDateTime}
import scala.language.implicitConversions

class ArgsAsClass(args:Array[String]) extends ArgsProcessor(args) {
  val testarg1:ArgumentT[Int]=ArgumentT.optional(0)
  val testarg2:ArgumentT[Boolean]=ArgumentT.required
  val testarg3:ArgumentT[String]=ArgumentT.static("AAA")

  private def setParams(): ArgsAsClass = {
    fillArgs()
    this
  }

  protected def getListOfFields: List[Field] = {
    val valList = this.getClass.getDeclaredFields
      .filterNot(this.getClass.getMethods.toSet)
      .filter(f => f.getType.equals(classOf[ArgumentT[_]]))
      .toList
    valList.foreach(_.setAccessible(true))
    valList
  }

  protected def fillArgs(): Unit =
    getListOfFields.foreach(field=>{
      val argField=field.get(this).asInstanceOf[ArgumentT[_]]
      argField.setName(field.getName)
      argField.setMap(asMap)
    })
}

object ArgsAsClass {
  def parse(args:Array[String]):ArgsAsClass = {
    new ArgsAsClass(args).setParams()
  }
}

class ArgumentT[T](val defaultValue:Option[T],val isRequired:Boolean) {
  private[this] var privName:Option[String]=None
  private[this] var pos:Option[Int]=None
  private[this] var valueMap:Option[Map[Either[String,Int],Argument]]=None

  lazy val name:String=privName.getOrElse("")

  def setName(newName:String):Unit =
    privName = if(privName.isEmpty) Some(newName) else privName

  def setPos(newPos:Int):Unit =
    pos = if(pos.isEmpty) Some(newPos) else pos

  def setMap(newMap:Map[Either[String,Int],Argument]):Unit =
    valueMap = if(valueMap.isEmpty) Some(newMap) else valueMap

  def arg:Option[Argument]=
    privName.flatMap(nm=>valueMap.flatMap(map=>map.get(Left(nm))))
}

object ArgumentT {
  def optional[T](defaultValue: T):ArgumentT[T]=new ArgumentT(Some(defaultValue),false)
  def required[T]:ArgumentT[T]=new ArgumentT(None,true)
  def static[T](value: T):ArgumentT[T]=new ArgumentT(Some(value),false)
  def ignored[T]:ArgumentT[T]=new ArgumentT(None,false)

  implicit def argToString(arg:ArgumentT[String]):String= argToType(arg,_.asString)
  implicit def argToInt(arg:ArgumentT[Int]):Int=argToType(arg,_.asInt)
  implicit def argToBoolean(arg:ArgumentT[Boolean]):Boolean=argToType(arg,_.asBoolean)
  implicit def argToLocalDate(arg:ArgumentT[LocalDate]):LocalDate=argToType(arg,_.asLocalDate)
  implicit def argToLocalDateTime(arg:ArgumentT[LocalDateTime]):LocalDateTime=argToType(arg,_.asLocalDateTime)

  private def argToType[T](arg:ArgumentT[T],asType:Argument=>Option[T]):T= {
    val v:Option[T]=arg.arg.flatMap(asType)
    (arg.isRequired,v.isEmpty,arg.defaultValue.isEmpty) match {
      case (true,true,_) => throw new IllegalArgumentException(f"Argument: '${arg.name}' not found")
      case (_,true,true) => throw new IllegalArgumentException(f"Argument: '${arg.name}' not found and is missing default value")
      case (_,false,_) => v.get
      case (_,true,false) => v.getOrElse(arg.defaultValue.get)
    }
  }
}