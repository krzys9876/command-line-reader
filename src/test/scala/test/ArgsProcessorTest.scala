package org.kr.args
package test

import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

class ArgsProcessorTest extends AnyFeatureSpec with GivenWhenThen {
  Feature("group args in logically related key-vaue pairs") {
    Scenario("positional args only") {
      Given("only positional args")
      val args = Array("aaa", "bbb", "ccc")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("only positional arguments are parsed")
      assert(processed.positional == args.toList)
      assert(processed.named.isEmpty)
    }
    Scenario("named args only with '='") {
      Given("only named args (--key=value)")
      val args = Array("-aaa=1", "-bbb=2", "--ccc=3")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("named arguments are parsed")
      assert(processed.named.toSet == Set(Argument("AAA", "1"), Argument("BBB", "2"), Argument("CCC", "3")))
      assert(processed.positional.isEmpty)
    }
    Scenario("named args only separated with space") {
      Given("only named args (--key value)")
      val args = Array("--aaa", "1", "--bbb", "2", "-ccc", "3")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("named arguments are parsed")
      assert(processed.named.toSet == Set(Argument("AAA", "1"), Argument("BBB", "2"), Argument("CCC", "3")))
      assert(processed.positional.isEmpty)
    }
    Scenario("named args separated with space or '='") {
      Given("only named args (--key value)")
      val args = Array("--aaa", "1", "--bbb=2", "-ccc", "3", "--ddd=4")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("named arguments are parsed")
      assert(processed.named.toSet == Set(Argument("AAA", "1"), Argument("BBB", "2"), Argument("CCC", "3"),Argument("DDD", "4")))
      assert(processed.positional.isEmpty)
    }
    Scenario("named args separated with space or '=' with positional args at the end") {
      Given("only named args (--key value)")
      val args = Array("--aaa", "1", "--bbb=2", "-ccc", "3", "--ddd=4","positional1","positional2")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("named arguments are parsed")
      assert(processed.named.toSet == Set(Argument("AAA", "1"), Argument("BBB", "2"), Argument("CCC", "3"),Argument("DDD", "4")))
      assert(processed.positional==List("positional1","positional2"))
    }
    Scenario("named args' names are converted to uppercase without separators") {
      Given("only named args with different cases and separators")
      val args = Array("--aa-a=1", "--bb-B=2", "-C_cC=3", "--dD.d=4","positional1","positional2")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("named arguments' names are converted to uppercase witout separators'")
      assert(processed.named.toSet == Set(Argument("AAA", "1"), Argument("BBB", "2"), Argument("CCC", "3"),Argument("DDD", "4")))
      assert(processed.positional==List("positional1","positional2"))
    }
    Scenario("named args and positional args with spaces in values") {
      Given("only named args with spaces in values (provided to OS with double quotes)")
      val args = Array("--aaa", "1 A", "--bbb=2 B", "-ccc", "3 C", "--ddd=4 D","positional 1","positional 2")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("named arguments are parsed")
      assert(processed.named.toSet == Set(Argument("AAA", "1 A"), Argument("BBB", "2 B"), Argument("CCC", "3 C"),Argument("DDD", "4 D")))
      assert(processed.positional==List("positional 1","positional 2"))
    }
    Scenario("named args after positional are treated as positional") {
      Given("only named args are provided after positional")
      val args = Array("positional1", "positional2", "--aaa", "1", "--bbb", "2", "-ccc", "3")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("all args are parsed as positional")
      assert(processed.named.isEmpty)
      assert(processed.positional == args.toList)
    }
    Scenario("a second value for a key indicates start of positional arguments") {
      Given("one of named arg has two values")
      val args = Array("--aaa=1", "--bbb", "2", "3", "-ccc", "4")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("only named args before doubled value are parsed as named, rest is positional")
      assert(processed.named.toSet == Set(Argument("AAA","1"),Argument("BBB","2")))
      assert(processed.positional == List("3", "-ccc", "4"))
    }
    Scenario("named arg may have empty value") {
      Given("named args with explicit empty value (e.g. key=)")
      val args = Array("--aaa=", "-bbb=2", "-ccc=")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("arguments with empty value are correctly passed")
      assert(processed.named.toSet == Set(Argument("AAA",""),Argument("BBB","2"),Argument("CCC","")))
      assert(processed.positional.isEmpty)
    }
  }
  Feature("access named and positional args in a unified way") {
    Scenario("access named args as option of value") {
      Given("named and positional args are provided")
      val args = Array("--aaa=5","--bbb=ABC","123","456")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("named args can be accessed directly, not via list")
      val aaa:Option[Int]=processed("AAA").flatMap(_.asInt)
      assert(aaa.contains(5))
      val bbb:Option[String]=processed("BBB").flatMap(_.asString)
      assert(bbb.contains("ABC"))
    }
    Scenario("access positional args as option of value") {
      Given("named and positional args are provided")
      val args = Array("--aaa=5","--bbb=ABC","123","XYZ")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("positional args can be accessed directly, not via list")
      val aaa:Option[Int]=processed(0).flatMap(_.asInt)
      assert(aaa.contains(123))
      val bbb:Option[String]=processed(1).flatMap(_.asString)
      assert(bbb.contains("XYZ"))
    }
  }
}
