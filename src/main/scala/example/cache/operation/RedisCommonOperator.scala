package example.cache.operation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.redis.serialization.{Format, Parse}
import com.redis.{RedisClient, RedisClientPool}

/**
 * A3用のRedisキャッシュシリアライズ、デシリアライズを定義したオブジェクト
 */
object A3Mapper {
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


/**
 * 最もベースとなるRedisを操作する設定やオペレーションを持つtrait
 * @tparam T 値として格納する値の型
 * Created by a12043 on 2014/10/14.
 */
trait RedisCommonOperator[T] {
  final val delimiter = "#"

  final val redisClientPool: RedisClientPool = new RedisClientPool("localhost", 6379)

  val dbNumber: Int

  val prefix: String

  val expireSec: Option[Int] = None

  val redisType: String

  var typeDef: Class[T]

  /**
   * キーや値のセリアライズに使うFormat
   */
  implicit val format = new Format(A3Mapper.format)

  /**
   * キーや値のデシリアライズに使うParse(typeDefに依存するためlazy)
   */
  implicit lazy val parse = new Parse[T](A3Mapper.parse[T](_: Array[Byte], typeDef))

  /**
   * Redisに実際に格納するキーを組み立てる関数
   * @param key アプリケーションから渡されるキー
   * @return 実際にRedisに保存されるキー
   */
  def setupCacheKey(key: String): String = {
    if (key == null || key.isEmpty) {
      throw new IllegalArgumentException("key string is not acceptable null.")
    }
    redisType + delimiter + prefix + delimiter + key
  }

  /**
   * 複数のキーが渡された時に実際にRedisに格納するキーに変換して返す関数
   * @param keys
   * @return
   */
  def setupCacheKeys(keys: Seq[String]): Seq[String] = {

    keys.isEmpty match {
      case true => null
      case false => keys.map(k => setupCacheKey(k))
    }
  }

  /**
   * RedisClientを使用する関数を、定められたDBのDBで実行するための関数
   * @param f
   * @tparam T
   * @return
   */
  protected def call[T](f: (RedisClient) => T): T = {
    redisClientPool.withClient(
      redisClient => {
        // 先にDB番号をセレクトしておく
        redisClient.select(dbNumber)
        // 実際の処理をコールする
        f(redisClient)
      }
    )
  }

  /**
   * 指定されたキー(単数もしくは複数)の値を削除します
   * @param key
   * @param keys
   * @return 削除された数
   */
  def delete(key: String, keys: String*): Option[Long] = {
    val cacheKey = setupCacheKey(key)
    if (keys == null || keys.isEmpty) {
      call[Option[Long]](_.del(cacheKey))
    } else {
      call[Option[Long]](_.del(cacheKey, setupCacheKeys(keys): _*))
    }
  }

  /**
   * 注意！！現在接続しているDBのすべてのキー、値を削除します
   * @return 成功時true
   */
  def flushDb(): Boolean = {
    call(_.flushdb)
  }

  def keyOf(value: T): String = {
    throw new UnsupportedOperationException(s"keyOf method is not implemented. $getClass")
  }
}

/**
 * Redisの文字列型に関する設定やオペレーションを持つtrait
 * @tparam T キャッシュとして格納する値の型
 */
trait RedisStringsOperator[T] extends RedisCommonOperator[T] {
  override val redisType = "STRINGS"

  def get(key: String): Option[T] = {
    call[Option[T]](
      _.get[T](setupCacheKey(key))
    )
  }

  def set(key: String, value: T): Boolean = {
    call[Boolean](
      client => {
        // expireSecが設定されている場合はsetexを呼ぶ
        expireSec match {
          case Some(x) => client.setex(setupCacheKey(key), x, value)
          case None => client.set(setupCacheKey(key), value)
        }
      }
    )
  }

  def set(value: T): Boolean = {
    set(keyOf(value), value);
  }
}

