package org.kr.args

import java.time.{LocalDate, LocalDateTime}

object Main extends App {

  // input: --a=123 --b "DEF GHI" -c=JKL -d T -e 2022-01-02 -f="2022-01-02 23:45:56" MNO PQR

  val sampleArgs=new SampleArgs(args)
  println(sampleArgs.named.mkString("|"))
  println(sampleArgs.positional.mkString("|"))

  val a:Int=sampleArgs.a
  val a1:Int=sampleArgs.a.value+1
  //sampleArgs.a.value
  println(a)
  println(a1)
  println(sampleArgs.a)
  println(sampleArgs.a())
  println(sampleArgs.a()+1)
  println(sampleArgs.b())
  println(sampleArgs.c)
  println(sampleArgs.d)
  println(sampleArgs.e)
  println(sampleArgs.f)
}

class SampleArgs(args:Array[String]) extends ArgsAsClass(args) {
  val a:ArgumentT[Int]=ArgumentT.required
  val b:ArgumentT[String]=ArgumentT.required
  val c:ArgumentT[String]=ArgumentT.required
  val d:ArgumentT[Boolean]=ArgumentT.optional(false)
  val e:ArgumentT[LocalDate]=ArgumentT.required
  val f:ArgumentT[LocalDateTime]=ArgumentT.required

  parse()
}

