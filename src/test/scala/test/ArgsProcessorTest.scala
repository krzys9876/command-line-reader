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
      assert(processed.named.toSet == Set(Argument("aaa", "1"), Argument("bbb", "2"), Argument("ccc", "3")))
      assert(processed.positional.isEmpty)
    }
    Scenario("named args only separated with space") {
      Given("only named args (--key value)")
      val args = Array("--aaa", "1", "--bbb", "2", "-ccc", "3")
      When("args are processed")
      val processed = new ArgsProcessor(args)
      Then("named arguments are parsed")
      assert(processed.named.toSet == Set(Argument("aaa", "1"), Argument("bbb", "2"), Argument("ccc", "3")))
      assert(processed.positional.isEmpty)
    }
    Scenario("named args separated with space or '='") {
      Given("only named args (--key value)")
      val args = Array("--aaa", "1", "--bbb=2", "-ccc", "3", "--ddd=4")
      When("args are processed")
      val processed = ArgsProcessor.parse(args)
      Then("named arguments are parsed")
      assert(processed.named.toSet == Set(Argument("aaa", "1"), Argument("bbb", "2"), Argument("ccc", "3"),Argument("ddd", "4")))
      assert(processed.positional.isEmpty)
    }
    Scenario("named args separated with space or '=' with positional args at the end") {
      Given("only named args (--key value)")
      val args = Array("--aaa", "1", "--bbb=2", "-ccc", "3", "--ddd=4","positional1","positional2")
      When("args are processed")
      val processed = ArgsProcessor.parse(args)
      Then("named arguments are parsed")
      assert(processed.named.toSet == Set(Argument("aaa", "1"), Argument("bbb", "2"), Argument("ccc", "3"),Argument("ddd", "4")))
      assert(processed.positional==List("positional1","positional2"))
    }
    Scenario("named args and positional args with spaces in values") {
      Given("only named args with spaces in values (provided to OS with double quotes)")
      val args = Array("--aaa", "1 A", "--bbb=2 B", "-ccc", "3 C", "--ddd=4 D","positional 1","positional 2")
      When("args are processed")
      val processed = ArgsProcessor.parse(args)
      Then("named arguments are parsed")
      assert(processed.named.toSet == Set(Argument("aaa", "1 A"), Argument("bbb", "2 B"), Argument("ccc", "3 C"),Argument("ddd", "4 D")))
      assert(processed.positional==List("positional 1","positional 2"))
    }
    Scenario("named args after positional are treated as positional") {
      Given("only named args are provided after positional")
      val args = Array("positional1", "positional2", "--aaa", "1", "--bbb", "2", "-ccc", "3")
      When("args are processed")
      val processed = ArgsProcessor.parse(args)
      Then("all args are parsed as positional")
      assert(processed.named.isEmpty)
      assert(processed.positional == args.toList)
    }
    Scenario("a second value for a key indicates start of positional arguments") {
      Given("one of named arg has two values")
      val args = Array("--aaa=1", "--bbb", "2", "3", "-ccc", "4")
      When("args are processed")
      val processed = ArgsProcessor.parse(args)
      Then("only named args before doubled value are parsed as named, rest is positional")
      assert(processed.named.toSet == Set(Argument("aaa","1"),Argument("bbb","2")))
      assert(processed.positional == List("3", "-ccc", "4"))
    }
    Scenario("named arg may include '-' in the middle of its names") {
      Given("named args with '-' in names")
      val args = Array("--aaa-a=1", "-bbb-b=2", "-ccc-c", "3","a-b-c-d-e-f")
      When("args are processed")
      val processed = ArgsProcessor.parse(args)
      Then("only named args before doubled value are parsed as named, rest is positional")
      assert(processed.named.toSet == Set(Argument("aaa-a","1"),Argument("bbb-b","2"),Argument("ccc-c","3")))
      assert(processed.positional == List("a-b-c-d-e-f"))
    }
    Scenario("named arg may have empty value") {
      Given("named args with explicit empty value (e.g. key=)")
      val args = Array("--aaa=", "-bbb=2", "-ccc=")
      When("args are processed")
      val processed = ArgsProcessor.parse(args)
      Then("arguments with empty value are correctly passed")
      assert(processed.named.toSet == Set(Argument("aaa",""),Argument("bbb","2"),Argument("ccc","")))
      assert(processed.positional.isEmpty)
    }
  }
  Feature("access named and positional args in a unified way") {
    Scenario("access named args as option of value") {
      Given("named and positional args are provided")
      val args = Array("--aaa=5","--bbb=ABC","123","456")
      When("args are processed")
      val processed = ArgsProcessor.parse(args)
      Then("named args can be accessed directly, not via list")
      val aaa:Option[Int]=processed("aaa").flatMap(_.asInt)
      assert(aaa.contains(5))
      val bbb:Option[String]=processed("bbb").flatMap(_.asString)
      assert(bbb.contains("ABC"))
    }
    Scenario("access positional args as option of value") {
      Given("named and positional args are provided")
      val args = Array("--aaa=5","--bbb=ABC","123","XYZ")
      When("args are processed")
      val processed = ArgsProcessor.parse(args)
      Then("positional args can be accessed directly, not via list")
      val aaa:Option[Int]=processed(0).flatMap(_.asInt)
      assert(aaa.contains(123))
      val bbb:Option[String]=processed(1).flatMap(_.asString)
      assert(bbb.contains("XYZ"))
    }
  }
}
