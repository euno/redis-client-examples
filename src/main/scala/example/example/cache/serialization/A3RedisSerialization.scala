package example.example.cache.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

object A3Mappeer {
  val objectMapper = (new ObjectMapper() with ScalaObjectMapper).registerModule(DefaultScalaModule)

  def apply(): ObjectMapper = objectMapper
}

/**
 * デシリアライズ用
 */
class A3RedisParse[A](t: Class[A]) {
  val typeDef: Class[A] = t

  val parse: (Array[Byte]) => A = {
    A3Mappeer().readValue[A](_, typeDef)
  }
}

/**
 * シリアライズ用
 */
object A3RedisFormat extends PartialFunction[Any, Any] {
  override def isDefinedAt(x: Any): Boolean = true

  override def apply(value: Any): Array[Byte] = {
    A3Mappeer().writeValueAsBytes(value)
  }
}