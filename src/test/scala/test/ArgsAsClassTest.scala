package org.kr.args
package test

import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

class ArgsAsClassTest extends AnyFeatureSpec with GivenWhenThen {
  Feature("arguments represented as class fields") {
    Scenario("assign arg values to corresponding fields") {
      Given("named arguments are provided")
      val args=Array("--test-arg-1","123","--test-arg-2","T")
      When("arguments are parsed")
      val argsClass=new Test01ArgsClass(args)
      Then("values from args are assigned to fields")
      assert(argsClass.testArg1==123)
      assert(argsClass.testArg2)
    }
  }
}

class Test01ArgsClass(args:Array[String]) extends ArgsProcessor(args) {
  lazy val testArg1:Int=ArgumentT(Argument("test-arg-1","123"),0)
  lazy val testArg2:Boolean=ArgumentT(Argument("test-arg-2","T"),false)
}

