package org.kr.args

import java.lang.reflect.Field
import java.time.{LocalDate, LocalDateTime}
import scala.language.implicitConversions
import scala.reflect.runtime.universe._

class ArgsAsClass(args:Array[String]) extends ArgsProcessor(args) {
  private def getListOfFields: List[Field] = {
    val valList = this.getClass.getDeclaredFields
      .filterNot(this.getClass.getMethods.toSet)
      .filter(f => f.getType.equals(classOf[ArgumentT[_]]))
      .toList
    valList.foreach(_.setAccessible(true))
    valList
  }

  private def fillArgs(): Unit =
    getListOfFields.foreach(field=>{
      val argField=field.get(this).asInstanceOf[ArgumentT[_]]
      argField.argumentType match {
        case NAMED=>
          argField.setName(field.getName)
          argField.setArg(this(field.getName))
        case POSITIONAL=>
          argField.setArg(this(argField.pos))
      }
    })

  def parse(): ArgsAsClass = {
    fillArgs()
    this
  }
}

sealed trait ArgumentType {
  val name:String
}

object NAMED extends ArgumentType {override val name:String="NAMED"}
object POSITIONAL extends ArgumentType {override val name:String="NAMED"}

class ArgumentT[T:TypeTag](val defaultValue:Option[T],val isRequired:Boolean,val argumentType:ArgumentType) {
  private[this] var privKey: Option[Either[String, Int]] = None
  private[this] var privArg: Option[Argument] = None

  // set name only once
  def setName(name: String): ArgumentT[T] = {
    privKey = privKey.orElse(Some(Left(name)))
    this
  }

  // set pos only once
  def setPos(pos: Int): ArgumentT[T] = {
    privKey = privKey.orElse(Some(Right(pos)))
    this
  }

  //set arg only once
  def setArg(newArg: Option[Argument]): ArgumentT[T] = {
    privArg = privArg.orElse(newArg)
    this
  }

  lazy val name: String = privKey.flatMap(_.left.toOption).getOrElse("")
  lazy val pos: Int = privKey.flatMap(_.swap.left.toOption).getOrElse(0)
  protected lazy val arg: Option[Argument] = privArg

  lazy val value:T= {
    val result=typeOf[T] match {
      case t if t=:= typeOf[Int] =>  ArgumentT.asInt(this.asInstanceOf[ArgumentT[Int]])
      case t if t=:= typeOf[String] => ArgumentT.asString(this.asInstanceOf[ArgumentT[String]])
      case t if t=:= typeOf[Boolean] => ArgumentT.asBoolean(this.asInstanceOf[ArgumentT[Boolean]])
      case t if t=:= typeOf[LocalDate] => ArgumentT.asLocalDate(this.asInstanceOf[ArgumentT[LocalDate]])
      case t if t=:= typeOf[LocalDateTime] => ArgumentT.asLocalDateTime(this.asInstanceOf[ArgumentT[LocalDateTime]])
    }
    result.asInstanceOf[T]
  }

  def apply():T=value

  override def toString: String = f"${if(argumentType==NAMED) f"name:$name" else f"pos:$pos"}  = $value"
 }

object ArgumentT {
  def optional[U:TypeTag](defaultValue: U):ArgumentT[U]=new ArgumentT(Some(defaultValue),false,NAMED)
  def required[U:TypeTag]:ArgumentT[U]=new ArgumentT(None,true,NAMED)
  def static[U:TypeTag](value: U):ArgumentT[U]=new ArgumentT(Some(value),false,NAMED)
  def ignored[U:TypeTag]:ArgumentT[U]=new ArgumentT(None,false,NAMED)

  def optionalPos[U:TypeTag](pos:Int,defaultValue: U):ArgumentT[U]=new ArgumentT(Some(defaultValue),false,POSITIONAL).setPos(pos)
  def requiredPos[U:TypeTag](pos:Int):ArgumentT[U]=new ArgumentT(None,true,POSITIONAL).setPos(pos)
  def staticPos[U:TypeTag](pos:Int,value: U):ArgumentT[U]=new ArgumentT(Some(value),false,POSITIONAL).setPos(pos)
  def ignoredPos[U:TypeTag](pos:Int):ArgumentT[U]=new ArgumentT(None,false,POSITIONAL).setPos(pos)

  implicit def asString(arg:ArgumentT[String]):String= argToType(arg,_.asString)
  implicit def asInt(arg:ArgumentT[Int]):Int=argToType(arg,_.asInt)
  implicit def asBoolean(arg:ArgumentT[Boolean]):Boolean=argToType(arg,_.asBoolean)
  implicit def asLocalDate(arg:ArgumentT[LocalDate]):LocalDate=argToType(arg,_.asLocalDate)
  implicit def asLocalDateTime(arg:ArgumentT[LocalDateTime]):LocalDateTime=argToType(arg,_.asLocalDateTime)

  private def argToType[U:TypeTag](arg:ArgumentT[U],asType:Argument=>Option[U]):U= {
    val v:Option[U]=arg.arg.flatMap(asType)
    (arg.arg.isEmpty,arg.isRequired,v.isEmpty,arg.defaultValue.isEmpty) match {
      case (true,true,_,_) => throw new ValueConversionForArgumentException(f"Missing argument: '${arg.name}'")
      case (false,true,true,_) => throw new MissingArgumentException(f"Incorrect value conversion for: '${arg.name}'")
      case (_,_,true,true) => throw new MissingArgumentException(f"Missing argument: '${arg.name}' without default value")
      case (_,_,false,_) => v.get
      case (_,_,true,false) => v.getOrElse(arg.defaultValue.get)
    }
  }
}

class MissingArgumentException(message : String) extends Exception(message) {}
class ValueConversionForArgumentException(message : String) extends Exception(message) {}