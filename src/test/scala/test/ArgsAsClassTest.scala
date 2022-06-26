package org.kr.args
package test

import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

import java.lang.reflect.Field
import java.time.{LocalDate, LocalDateTime}
import scala.language.implicitConversions

class ArgsAsClassTest extends AnyFeatureSpec with GivenWhenThen {
  Feature("arguments represented as class fields") {
    Scenario("assign arg values to corresponding fields") {
      Given("named arguments are provided")
      val args=Array("--testarg1","123","--testarg2","T")
      When("arguments are parsed")
      val argsClass=Test01ArgsClass.parse(args)
      Then("values from args are assigned to fields")

      val arg1:Int=argsClass.testarg1
      assert(arg1==123)
      val arg2:Boolean=argsClass.testarg2
      assert(arg2)
      val arg3:String=argsClass.testarg3
      assert(arg3=="AAA")
    }
  }
}

class Test01ArgsClass(args:Array[String]) extends ArgsProcessor(args) {
  val testarg1:ArgumentT[Int]=ArgumentT.optional(0)
  val testarg2:ArgumentT[Boolean]=ArgumentT.required
  val testarg3:ArgumentT[String]=ArgumentT.static("AAA")

  private def setParams(): Test01ArgsClass = {
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

object Test01ArgsClass {
  def parse(args:Array[String]):Test01ArgsClass = {
    new Test01ArgsClass(args).setParams()
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