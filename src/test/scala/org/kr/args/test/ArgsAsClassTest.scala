package org.kr.args.test

import org.kr.args.{ArgsAsClass, Argument, MissingArgumentException, PrintHelpAndExit}
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

import java.time.{LocalDate, LocalDateTime}
import scala.util.Try

class ArgsAsClassTest extends AnyFeatureSpec with GivenWhenThen {
  Feature("arguments represented as class fields") {
    Scenario("access named arg values using corresponding fields ") {
      Given("named arguments are provided")
      And("a value for boolean parameter is provided")
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
      val argPos2AsField:Long=argsClass.posarg2
      assert(argPos2AsField==9999)
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
  Feature("parse different types of arguments") {
    Scenario("parse Int") {
      Given("named argument is provided")
      val args=Array("--arg.int=321")
      When("arguments are parsed")
      val argsClass=new ArgsTest02(args)
      Then("value is properly parsed")
      And("can be accessed implicitly")
      val v:Int=argsClass.argInt
      assert(v==321)
      And("explicitly")
      assert(argsClass.argInt()==321)
    }
    Scenario("parse String") {
      Given("named argument is provided")
      val args=Array("--arg.string=ABC DEF")
      When("arguments are parsed")
      val argsClass=new ArgsTest02(args)
      Then("value is properly parsed")
      And("can be accessed implicitly")
      val v:String=argsClass.argString
      assert(v=="ABC DEF")
      And("explicitly")
      assert(argsClass.argString()=="ABC DEF")
    }
    Scenario("parse Boolean") {
      Given("named argument is provided")
      val args=Array("--arg.boolean=T")
      When("arguments are parsed")
      val argsClass=new ArgsTest02(args)
      Then("value is properly parsed")
      And("can be accessed implicitly")
      val v:Boolean=argsClass.argBoolean
      assert(v)
      And("explicitly")
      assert(argsClass.argBoolean())
    }
    Scenario("parse LocalDate") {
      Given("named argument is provided")
      val args=Array("--arg.local.date=2020-12-31")
      When("arguments are parsed")
      val argsClass=new ArgsTest02(args)
      Then("value is properly parsed")
      And("can be accessed implicitly")
      val v:LocalDate=argsClass.argLocalDate
      assert(v==LocalDate.of(2020,12,31))
      And("explicitly")
      assert(argsClass.argLocalDate()==LocalDate.of(2020,12,31))
    }
    Scenario("parse LocalDateTime") {
      Given("named argument is provided")
      val args=Array("--arg.local.date.time=2021-12-31 23:59:59")
      When("arguments are parsed")
      val argsClass=new ArgsTest02(args)
      Then("value is properly parsed")
      And("can be accessed implicitly")
      val v:LocalDateTime=argsClass.argLocalDateTime
      assert(v==LocalDateTime.of(2021,12,31,23,59,59))
      And("explicitly")
      assert(argsClass.argLocalDateTime()==LocalDateTime.of(2021,12,31,23,59,59))
    }
    Scenario("parse Double") {
      Given("named argument is provided")
      val args=Array("--arg.double=3.1415")
      When("arguments are parsed")
      val argsClass=new ArgsTest02(args)
      Then("value is properly parsed")
      And("can be accessed implicitly")
      val v:Double=argsClass.argDouble
      assert(v==3.1415)
      And("explicitly")
      assert(argsClass.argDouble()==3.1415)
    }
    Scenario("missing required argument") {
      Given("named argument is not provided")
      val args = Array("--test.arg.2=YYY")
      When("arguments are parsed")
      Then("exception is thrown")
      assertThrows[MissingArgumentException](new ArgsTest03(args))
    }
    Scenario("single boolean argument w/o value") {
      Given("named boolean argument is provided w/o value (i.e. as true)")
      val args = Array("--test.arg.1")
      When("arguments are parsed")
      val argsClass=new ArgsTest04(args)
      Then("value is set to true")
      assert(argsClass.testArg1())
    }
    Scenario("more boolean arguments w/o values") {
      Given("named boolean arguments are provided w/o values (i.e. as true)")
      val args = Array("--test.arg.1","--test.arg.2","--test.arg.3","123","--test.arg.4")
      When("arguments are parsed")
      val argsClass = new ArgsTest05(args)
      Then("value is set to true")
      assert(argsClass.testArg1())
      assert(argsClass.testArg2())
      assert(argsClass.testArg3()==123)
      assert(argsClass.testArg4())
    }
  }
  Feature("print usage instruction") {
    Scenario("print generated usage instructions with correct parameter names") {
      Given("a class with args")
      val args = Array("--testarg1", "123", "--testarg2", "T", "uvw", "xyz")
      val argsClass = new ArgsTest01(args)
      When("instruction is printed")
      Then("it reflects field names and descriptions")
      assert(argsClass.testarg1.suggestedInputName == "testarg1")
      assert(argsClass.testArg2.suggestedInputName == "test-arg2")
      assert(argsClass.test_arg_3.suggestedInputName == "test-arg-3")
      assert(argsClass.TESTARG_4.suggestedInputName == "testarg-4")
      assert(argsClass.testarg5.suggestedInputName == "testarg5")
      assert(argsClass.posarg0.suggestedInputName == "(1)")
      assert(argsClass.posarg1.suggestedInputName == "(2)")
      assert(argsClass.posarg2.suggestedInputName == "(3)")
      assert(argsClass.posarg3.suggestedInputName == "(4)")
      assert(argsClass.posarg4.suggestedInputName == "(5)")
      val instruction=argsClass.instruction
      assert(instruction.contains(argsClass.testarg1.suggestedInputName))
      assert(instruction.contains(argsClass.testArg2.suggestedInputName))
      assert(instruction.contains(argsClass.test_arg_3.suggestedInputName))
      assert(instruction.contains(argsClass.TESTARG_4.suggestedInputName))
      assert(instruction.contains(argsClass.testarg5.suggestedInputName))
      assert(instruction.contains(argsClass.posarg0.suggestedInputName))
      assert(instruction.contains(argsClass.posarg1.suggestedInputName))
      assert(instruction.contains(argsClass.posarg2.suggestedInputName))
      assert(instruction.contains(argsClass.posarg3.suggestedInputName))
      assert(instruction.contains(argsClass.posarg4.suggestedInputName))
    }
    Scenario("print usage instructions when only --help is provided ") {
      Given("a class with only one arg: --hep")
      val args = Array("--help")
      When("arguments are parsed")
      val argsAsClass = Try {new ArgsTest04(args)}
      Then("a special type of exception is thrown")
      assert(argsAsClass.isFailure)
      assert(argsAsClass.toEither.left.toOption.get.getMessage.contains("[--test-arg1]"))
    }
  }
}

class ArgsTest01(args:Array[String]) extends ArgsAsClass(args) {
  val testarg1:Argument[Int]=Argument.optional(0,"instruction 1")
  val testArg2:Argument[Boolean]=Argument.required("instruction 2")
  val test_arg_3:Argument[String]=Argument.optional("AAA")
  val TESTARG_4:Argument[String]=Argument.static("BBB","instruction 4")
  val testarg5:Argument[LocalDate]=Argument.ignored

  val posarg0:Argument[String]=Argument.requiredPos(0)
  val posarg1:Argument[String]=Argument.requiredPos(1,"instruction pos. 2")
  val posarg2:Argument[Long]=Argument.optionalPos(2,9999)
  val posarg3:Argument[String]=Argument.staticPos(3,"def","instruction pos. 4")
  val posarg4:Argument[String]=Argument.ignoredPos(4)
  parse()
}

class ArgsTest02(args:Array[String]) extends ArgsAsClass(args) {
  val argInt:Argument[Int]=Argument.optional(1)
  val argString:Argument[String]=Argument.optional("X")
  val argBoolean:Argument[Boolean]=Argument.optional(false)
  val argLocalDate:Argument[LocalDate]=Argument.optional(LocalDate.of(2022,3,4))
  val argLocalDateTime:Argument[LocalDateTime]=Argument.optional(LocalDateTime.of(2022,4,5,12,23,34))
  val argDouble:Argument[Double]=Argument.optional(1.23)

  parse()
}

class ArgsTest03(args:Array[String]) extends ArgsAsClass(args) {
  val testArg1:Argument[Boolean]=Argument.required
  val testArg2:Argument[String]=Argument.optional("AAA")
  parse()
}

class ArgsTest04(args:Array[String]) extends ArgsAsClass(args) {
  val testArg1:Argument[Boolean]=Argument.optional(false)
  parse()
}

class ArgsTest05(args:Array[String]) extends ArgsAsClass(args) {
  val testArg1:Argument[Boolean]=Argument.optional(false)
  val testArg2:Argument[Boolean]=Argument.optional(false)
  val testArg3:Argument[Int]=Argument.required
  val testArg4:Argument[Boolean]=Argument.optional(false)
  parse()
}
