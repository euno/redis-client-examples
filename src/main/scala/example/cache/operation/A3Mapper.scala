package example.cache.operation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import scala.reflect.ClassTag

/**
 * A3用のRedisキャッシュシリアライズ、デシリアライズを定義したオブジェクト
 */
object A3Mapper {
  val objectMapper = (new ObjectMapper() with ScalaObjectMapper).registerModule(DefaultScalaModule)

  def parse[A](value: Array[Byte], typeDef: Class[A]): A = {
    //val des =
    objectMapper.readValue[A](value, typeDef)
    //typeDef.is
    //List(1,2,3).zipWithIndex
  }

  /*
  def isTuple(typeDef: Class[A]): Boolean = {
    typeDef match {
      case
    }
  }
  */

  val format: PartialFunction[Any, Any] = new PartialFunction[Any, Any] {
    override def isDefinedAt(x: Any): Boolean = true

    override def apply(value: Any): Array[Byte] = {
      objectMapper.writeValueAsBytes(value)
    }
  }
}
