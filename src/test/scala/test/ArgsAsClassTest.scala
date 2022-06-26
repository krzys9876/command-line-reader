package org.kr.args
package test

import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

import java.time.{LocalDate, LocalDateTime}
import scala.language.implicitConversions

class ArgsAsClassTest extends AnyFeatureSpec with GivenWhenThen {
  Feature("arguments represented as class fields") {
    Scenario("assign arg values to corresponding fields") {
      Given("named arguments are provided")
      val args=Array("--testarg1","123","--testarg2","T")
      When("arguments are parsed")
      val argsClass=new Test01ArgsClass(args)
      Then("values from args are assigned to fields")

      val arg1:Int=argsClass.testarg1
      assert(arg1==123)
      val arg2:Boolean=argsClass.testarg2
      assert(arg2)
    }
  }
}

class Test01ArgsClass(args:Array[String]) extends ArgsProcessor(args) {
  setParams()

  private def setParams(): Unit = {
    // TODO: to be replaced using reflection
    testarg1.setName("testarg1")
    testarg2.setName("testarg2")
    testarg1.setMap(asMap)
    testarg2.setMap(asMap)
  }


  lazy val testarg1:ArgumentT[Int]=ArgumentT.optional(0)
  lazy val testarg2:ArgumentT[Boolean]=ArgumentT.optional(false)


}

object Test01ArgsClass {
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