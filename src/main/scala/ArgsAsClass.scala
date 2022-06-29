package org.kr.args

import java.lang.reflect.Field
import scala.language.implicitConversions
import scala.reflect.runtime.universe._

class ArgsAsClass(args:Array[String]) extends ArgsProcessor(args) {
  private def getListOfFields: List[Field] = {
    val valList = this.getClass.getDeclaredFields
      .filterNot(this.getClass.getMethods.toSet)
      .filter(f => f.getType.equals(classOf[Argument[_]]))
      .toList
    valList.foreach(_.setAccessible(true))
    valList
  }

  private def fillArgs(): Unit =
    getListOfFields.foreach(field=>{
      val argField=field.get(this).asInstanceOf[Argument[_]]
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

class Argument[T:TypeTag](val defaultValue:Option[T], val isRequired:Boolean, val argumentType:ArgumentType) {
  private[this] var privKey: Option[Either[String, Int]] = None
  private[this] var privArg: Option[RawArgument] = None

  // set name only once
  def setName(name: String): Argument[T] = {
    privKey = privKey.orElse(Some(Left(name)))
    this
  }
  // set pos only once
  def setPos(pos: Int): Argument[T] = {
    privKey = privKey.orElse(Some(Right(pos)))
    this
  }
  //set arg only once
  def setArg(newArg: Option[RawArgument]): Argument[T] = {
    privArg = privArg.orElse(newArg)
    this
  }

  lazy val name: String = privKey.flatMap(_.left.toOption).getOrElse("")
  lazy val pos: Int = privKey.flatMap(_.swap.left.toOption).getOrElse(0)
  protected lazy val arg: Option[RawArgument] = privArg

  lazy val value:T= Argument.asValueUnsafe[T](this)
  lazy val optValue:Option[T]= Argument.asOption[T](this)
  def apply():T=value

  override def toString: String = f"${if(argumentType==NAMED) f"name:$name" else f"pos:$pos"}  = $value"
 }

object Argument {
  def optional[U:TypeTag](defaultValue: U):Argument[U]=new Argument(Some(defaultValue),false,NAMED)
  def required[U:TypeTag]:Argument[U]=new Argument(None,true,NAMED)
  def static[U:TypeTag](value: U):Argument[U]=new Argument(Some(value),false,NAMED)
  def ignored[U:TypeTag]:Argument[U]=new Argument(None,false,NAMED)

  def optionalPos[U:TypeTag](pos:Int,defaultValue: U):Argument[U]=new Argument(Some(defaultValue),false,POSITIONAL).setPos(pos)
  def requiredPos[U:TypeTag](pos:Int):Argument[U]=new Argument(None,true,POSITIONAL).setPos(pos)
  def staticPos[U:TypeTag](pos:Int,value: U):Argument[U]=new Argument(Some(value),false,POSITIONAL).setPos(pos)
  def ignoredPos[U:TypeTag](pos:Int):Argument[U]=new Argument(None,false,POSITIONAL).setPos(pos)

  // straightforward implicit conversion between typed argument and option of its value type
  implicit def asOption[U: TypeTag](arg:Argument[U])(implicit f:Option[RawArgument]=>Option[U]) :Option[U]= RawArgument.argToType(arg.arg)
  // unsafe implicit conversion between typed argument and its value type -
  // throws error if a value that should be returned is missing (either missing arg or missing default value)
  implicit def asValueUnsafe[U: TypeTag](arg:Argument[U])(implicit f:Option[RawArgument]=>Option[U]) :U= {
    val v:Option[U]=asOption(arg)
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