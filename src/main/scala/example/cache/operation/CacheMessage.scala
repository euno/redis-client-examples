package example.cache.operation

import org.msgpack.annotation.Message

/**
 * Created by a12043 on 2014/10/14.
 */
@Message
class CacheMessage[A](obj: A) {
  val value: A = obj
}
