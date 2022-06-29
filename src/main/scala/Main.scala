package org.kr.args

import java.time.{LocalDate, LocalDateTime}

object Main extends App {
  // input: --a=123 --b "DEF GHI" -c=JKL -d T -e 2022-01-02 -f="2022-01-02 23:45:56" MNO PQR

  val sampleArgs=new SampleArgs(args)
  println(sampleArgs.named.mkString("|"))
  println(sampleArgs.positional.mkString("|"))

  // implicit conversion to typed val (it must be typed for compiler to infer destination type)
  val a:Int=sampleArgs.a
  // explicit access to value
  val a1:Int=sampleArgs.a.value+1
  // impicit access to option of value - None means missing arg or missing default value
  val a2:Option[Int]=sampleArgs.a
  val a3:Int=sampleArgs.a.optValue.getOrElse(5)

  // let's print various combinations of argument values
  println(f"a:$a")
  println(f"a1:$a1")
  println(f"a2:$a2")
  println(sampleArgs.a)
  println(sampleArgs.a())
  println(sampleArgs.a.value)
  println(sampleArgs.a()+1)
  println(sampleArgs.b)
  println(sampleArgs.b())
  println(sampleArgs.c)
  println(sampleArgs.d)
  println(sampleArgs.e)
  println(sampleArgs.f)
  println(sampleArgs.g)

  println(sampleArgs.pos0)
  println(sampleArgs.pos0())
  println(sampleArgs.pos1)
  println(sampleArgs.pos1())
  println(sampleArgs.pos2)
  println(sampleArgs.pos2())

  val sampleArgs2=new SampleArgs2(Array("-double.value=1.234E-3"))
  val d:Double=sampleArgs2.doubleValue
  println(d)
}

class SampleArgs(args:Array[String]) extends ArgsAsClass(args) {
  val a:Argument[Int]=Argument.required
  val b:Argument[String]=Argument.required
  val c:Argument[String]=Argument.required
  val d:Argument[Boolean]=Argument.optional(false)
  val e:Argument[LocalDate]=Argument.required
  val f:Argument[LocalDateTime]=Argument.required

  val pos0:Argument[String]=Argument.requiredPos(0)
  val pos1:Argument[String]=Argument.requiredPos(1)
  val pos2:Argument[String]=Argument.optionalPos(2,"none")

  val g:Argument[LocalDateTime]=Argument.static(LocalDateTime.now())

  parse()
}

class SampleArgs2(args:Array[String]) extends ArgsAsClass(args) {
  val doubleValue:Argument[Double]=Argument.required
  parse()
}
