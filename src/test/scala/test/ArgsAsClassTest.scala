package org.kr.args
package test

import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

class ArgsAsClassTest extends AnyFeatureSpec with GivenWhenThen {
  Feature("arguments represented as class fields") {
    Scenario("assign arg values to corresponding fields") {
      Given("named arguments are provided")
      val args=Array("--testarg1","123","--testarg2","T")
      When("arguments are parsed")
      val argsClass=ArgsAsClass.parse(args)
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
