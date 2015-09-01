package io.getquill.source.mirror

import scala.reflect.macros.whitebox.Context
import language.experimental.macros
import io.getquill.ast.Ast
import io.getquill.quotation.Quoted
import io.getquill.source.Source
import io.getquill.source.SourceMacro
import io.getquill.util.Messages._
import scala.reflect.ClassTag

case class Row(data: Any*) {
  def add(value: Any) = Row((data :+ value): _*)
  def apply[T](index: Int)(implicit t: ClassTag[T]) =
    data(index) match {
      case v: T  => v
      case other => fail(s"Invalid column type. Expected '${t.runtimeClass}', but got '$other'")
    }
}

object mirrorSource extends Source[Row, Row] {

  def run[T](quoted: Quoted[T]): Any = macro MirrorSourceMacro.run[Row, Row, T]

  def mirrorConfig = config

  case class ActionMirror(ast: Ast)

  def execute(ast: Ast) =
    ActionMirror(ast)

  case class BatchActionMirror(ast: Ast, bindList: List[Row])

  def execute(ast: Ast, bindList: List[Row => Row]) =
    BatchActionMirror(ast, bindList.map(_(Row())))

  case class QueryMirror[T](ast: Ast, binds: Row, extractor: Row => T)

  def query[T](ast: Ast, bind: Row => Row, extractor: Row => T) =
    QueryMirror(ast, bind(Row()), extractor)

  implicit val longDecoder = new Decoder[Long] {
    def apply(index: Int, row: Row) =
      row[Long](index)
  }

  implicit val longEncoder = new Encoder[Long] {
    def apply(index: Int, value: Long, row: Row) =
      row.add(value)
  }

  implicit val intDecoder = new Decoder[Int] {
    def apply(index: Int, row: Row) =
      row[Int](index)
  }

  implicit val intEncoder = new Encoder[Int] {
    def apply(index: Int, value: Int, row: Row) =
      row.add(value)
  }

  implicit val stringDecoder = new Decoder[String] {
    def apply(index: Int, row: Row) =
      row[String](index)
  }

  implicit val stringEncoder = new Encoder[String] {
    def apply(index: Int, value: String, row: Row) =
      row.add(value)
  }
}

class MirrorSourceMacro(val c: Context) extends SourceMacro {
  import c.universe._
  override protected def toExecutionTree(ast: Ast) = {
    c.info(ast.toString)
    q"$ast"
  }
}
