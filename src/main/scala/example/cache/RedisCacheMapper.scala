package example.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

/**
  * Redisのキャッシュ用のシリアライズ、デシリアライズを行う関数を持つオブジェクト
  */
object RedisCacheMapper {
   val objectMapper = (new ObjectMapper() with ScalaObjectMapper).registerModule(DefaultScalaModule)

   def parse[A](value: Array[Byte], typeDef: Class[A]): A = {
     objectMapper.readValue[A](value, typeDef)
   }

   val format: PartialFunction[Any, Any] = new PartialFunction[Any, Any] {
     override def isDefinedAt(x: Any): Boolean = true

     override def apply(value: Any): Array[Byte] = {
       objectMapper.writeValueAsBytes(value)
     }
   }
 }
