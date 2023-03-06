package org.kr.args

import java.lang.reflect.Field
import scala.language.implicitConversions

class ArgsAsClass(args:Array[String],instructionHeader:String="") extends ArgsProcessor(args) {
  private val help:Argument[Boolean] = Argument.optional(false,"Prints usage instructions")

  private def getListOfFields: List[Field] = {
    val valList = (this.getClass.getDeclaredFields ++ this.getClass.getSuperclass.getDeclaredFields)
      .filterNot(this.getClass.getMethods.toSet)
      .filterNot(this.getClass.getSuperclass.getMethods.toSet)
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

  private def verifyArgs(): Unit = {
    if (help.value) throw new PrintHelpAndExit(instruction)
    else getListOfFields.foreach(field => {
      val argField = field.get(this).asInstanceOf[Argument[_]]
      if (argField.isRequired) argField.value // evaluate required arguments
    })
  }


  private def singleEntry(arg: Argument[_]): String = {
    val prefix=if(arg.argumentType == NAMED) "--" else ""
    val name=prefix+arg.suggestedInputName
  //  f"${arg.suggestedInputName} | ${if (arg.isRequired) "required" else "optional, default: "+arg.defaultValue.getOrElse("")} | ${arg.converter.name} | ${arg.instruction.getOrElse("")}\n"
    List(
      if (arg.isRequired) f" $name " else f"[$name] (default: ${arg.defaultValue.getOrElse("")})",
      arg.converter.name,
      arg.instruction.getOrElse(""))
      .mkString(" | ")+"\n"
  }

  def instruction:String = {
    val fieldInstructions = getListOfFields.foldLeft("")((instr,field) => {
      val argField = field.get(this).asInstanceOf[Argument[_]]
      instr+singleEntry(argField)
    })
    f"""
      |${if(instructionHeader.nonEmpty) instructionHeader else "Usage"}
      | name | type | description
      |
      |$fieldInstructions
      |""".stripMargin
  }

  def parse(): ArgsAsClass = {
    fillArgs()
    verifyArgs()
    this
  }
}

sealed trait ArgumentType {
  val name:String
}

object NAMED extends ArgumentType {override val name:String="NAMED"}
object POSITIONAL extends ArgumentType {override val name:String="NAMED"}

class Argument[T](val defaultValue:Option[T], val isRequired:Boolean, val argumentType:ArgumentType, val instruction:Option[String]=None)
                 (implicit val converter:RawArgumentConverter[T]) {
  private[this] var privKey: Option[Either[String, Int]] = None
  private[this] var privArg: Option[RawArgument] = None
  private[this] var privInputName:Option[String]  = None


  // set name only once
  def setName(name: String): Argument[T] = {
    privKey = privKey.orElse(Some(Left(name)))
    privInputName = privInputName.orElse(Some(ArgsProcessor.unformatName(name)))
    this
  }
  // set pos only once
  private def setPos(pos: Int): Argument[T] = {
    privKey = privKey.orElse(Some(Right(pos)))
    this
  }
  //set arg only once
  def setArg(newArg: Option[RawArgument]): Argument[T] = {
    privArg = privArg.orElse(newArg)
    this
  }

  lazy val name: String = privKey.flatMap(_.left.toOption).getOrElse("")
  lazy val suggestedInputName: String = argumentType match {
    case POSITIONAL => f"(${pos+1})"
    case NAMED => privInputName.getOrElse("")
  }
  lazy val pos: Int = privKey.flatMap(_.swap.left.toOption).getOrElse(0)
  protected lazy val arg: Option[RawArgument] = privArg

  lazy val optValue:Option[T]=converter.toValue(arg)
  lazy val value:T= {
    val v:Option[T]=converter.toValue(arg)
    (arg.isEmpty,isRequired,v.isEmpty,defaultValue.isEmpty) match {
      case (true,true,_,_) => throw new MissingArgumentException(f"Incorrect value conversion for: '$name'")
      case (false,true,true,_) => throw new ValueConversionForArgumentException(f"Missing argument: '$name'")
      case (_,_,true,true) => throw new MissingArgumentException(f"Missing argument: '$name' without default value")
      case (_,_,false,_) => v.get
      case (_,_,true,false) => v.getOrElse(defaultValue.get)
    }
  }
  def apply():T=value

  override def toString: String = f"${if(argumentType==NAMED) f"name:$name" else f"pos:$pos"}  = $value"
 }

object Argument {
  def optional[U](defaultValue: U, instruction:String="")(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(Some(defaultValue),false,NAMED, instructionAsOption(instruction))
  def required[U](implicit converter: RawArgumentConverter[U]): Argument[U] =
    new Argument(None, true, NAMED)
  def required[U](instruction:String)(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(None,true,NAMED, instructionAsOption(instruction))
  def static[U](value: U, instruction:String="")(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(Some(value),false,NAMED, instructionAsOption(instruction))
  def ignored[U](implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(None,false,NAMED)

  def optionalPos[U](pos:Int,defaultValue: U,instruction:String="")(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(Some(defaultValue),false,POSITIONAL, instructionAsOption(instruction)).setPos(pos)
  def requiredPos[U](pos:Int,instruction:String="")(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(None,true,POSITIONAL, instructionAsOption(instruction)).setPos(pos)
  def staticPos[U](pos:Int,value: U,instruction:String="")(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(Some(value),false,POSITIONAL, instructionAsOption(instruction)).setPos(pos)
  def ignoredPos[U](pos:Int,instruction:String="")(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(None,false,POSITIONAL, instructionAsOption(instruction)).setPos(pos)

  private def instructionAsOption(instruction:String):Option[String] = if(instruction.isEmpty) None else Some(instruction)

  //implicit conversion between typed argument and option of its value
  implicit def asOption[U](arg:Argument[U]) :Option[U]= arg.optValue
  //implicit (unsafe) conversion between typed argument and its value
  implicit def asValueUnsafe[U](arg:Argument[U]) :U= arg.value
}

class MissingArgumentException(message : String) extends Exception(message) {}
class ValueConversionForArgumentException(message : String) extends Exception(message) {}
class PrintHelpAndExit(message : String) extends Exception(message) {}
