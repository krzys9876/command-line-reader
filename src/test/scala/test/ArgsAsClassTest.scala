package org.kr.args
package test

import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

import java.time.LocalDate

class ArgsAsClassTest extends AnyFeatureSpec with GivenWhenThen {
  Feature("arguments represented as class fields") {
    Scenario("access named arg values using corresponding fields ") {
      Given("named arguments are provided")
      val args=Array("--testarg1","123","--testarg2","T","uvw","xyz")
      When("arguments are parsed")
      val argsClass=new ArgsTest01(args)
      Then("values from args are accessible through fields")
      And("field names may contain upper and lower case letters and underscore")
      val arg1:Int=argsClass.testarg1
      assert(arg1==123)
      val arg2:Boolean=argsClass.testArg2
      assert(arg2)
      val arg3:String=argsClass.test_arg_3
      assert(arg3=="AAA")
    }
    Scenario("access optional named arg values using corresponding fields") {
      Given("named argument is not provided")
      val args=Array("--testarg1","123","--testarg2","T","uvw","xyz")
      When("arguments are parsed")
      val argsClass=new ArgsTest01(args)
      Then("default value is accessible through field")
      And("field names may contain upper and lower case letters and underscore")
      val arg3:String=argsClass.test_arg_3
      assert(arg3=="AAA")
      val arg4:String=argsClass.TESTARG_4
      assert(arg4=="BBB")
    }
    Scenario("cannot access ignored named arg values using corresponding fields") {
      Given("named argument is not provided")
      val args=Array("--testarg1","123","--testarg2","T","uvw","xyz")
      When("arguments are parsed")
      val argsClass=new ArgsTest01(args)
      Then("error is thrown when ignored arg is accessed")
      assertThrows[MissingArgumentException]({val arg5:LocalDate=argsClass.testarg5})
    }
    Scenario("access positional arg values to corresponding fields") {
      Given("positional arguments are provided")
      val args=Array("--testarg1","123","--testarg2","T","uvw","xyz")
      When("arguments are parsed")
      val argsClass=new ArgsTest01(args)
      Then("values from args are assigned to fields")
      val argPos0:String=argsClass.positional.head
      assert(argPos0=="uvw")
      val argPos0AsField:String=argsClass.posarg0
      assert(argPos0AsField=="uvw")
      val argPos1:String=argsClass.positional(1)
      assert(argPos1=="xyz")
      val argPos1AsField:String=argsClass.posarg1
      assert(argPos1AsField=="xyz")
    }
    Scenario("access optional positional arg values to corresponding fields") {
      Given("positional arguments are not provided")
      val args=Array("--testarg1","123","--testarg2","T","uvw","xyz")
      When("arguments are parsed")
      val argsClass=new ArgsTest01(args)
      Then("values from args are assigned to fields")
      val argPos2AsField:String=argsClass.posarg2
      assert(argPos2AsField=="abc")
      val argPos3AsField:String=argsClass.posarg3
      assert(argPos3AsField=="def")
    }
    Scenario("cannot access ignored positional arg values using corresponding fields") {
      Given("positional argument is not provided")
      val args=Array("--testarg1","123","--testarg2","T","uvw","xyz")
      When("arguments are parsed")
      val argsClass=new ArgsTest01(args)
      Then("values from args are assigned to fields")
      assertThrows[MissingArgumentException]({val posarg4:String=argsClass.posarg4})
    }
  }
}

class ArgsTest01(args:Array[String]) extends ArgsAsClass(args) {
  val testarg1:ArgumentT[Int]=ArgumentT.optional(0)
  val testArg2:ArgumentT[Boolean]=ArgumentT.required
  val test_arg_3:ArgumentT[String]=ArgumentT.optional("AAA")
  val TESTARG_4:ArgumentT[String]=ArgumentT.static("BBB")
  val testarg5:ArgumentT[LocalDate]=ArgumentT.ignored

  val posarg0:ArgumentT[String]=ArgumentT.requiredPos(0)
  val posarg1:ArgumentT[String]=ArgumentT.requiredPos(1)
  val posarg2:ArgumentT[String]=ArgumentT.optionalPos(2,"abc")
  val posarg3:ArgumentT[String]=ArgumentT.staticPos(3,"def")
  val posarg4:ArgumentT[String]=ArgumentT.ignoredPos(4)
  parse()
}
