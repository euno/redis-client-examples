package example.cache.operation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.redis.serialization.{Format, Parse}
import com.redis.{RedisClient, RedisClientPool}
import example.example.cache.serialization.{A3RedisFormat, A3RedisParse}

/**
 * 最もベースとなるRedisを操作する設定やオペレーションを持つtrait
 * @tparam T 値として格納する値の型
 * Created by a12043 on 2014/10/14.
 */
sealed trait RedisCommonOperator[T] {
  final val delimiter = "#"

  val dbNumber: Int

  val prefix: String

  val expireSec: Option[Int]

  val redisClientPool: RedisClientPool

  val redisType: String

  val typeDef: Class[T]

  /**
   * キーや値のセリアライズに使うFormat
   */
  implicit val format = new Format(A3RedisFormat)

  /**
   * キーや値のデシリアライズに使うParse(typeDefに依存するためlazy)
   */
  implicit lazy val parse = new Parse[T](new A3RedisParse[T](typeDef).parse)

  /**
   * Redisに実際に格納するキーを組み立てる関数
   * @param key アプリケーションから渡されるキー
   * @return 実際にRedisに保存されるキー
   */
  def setupCacheKey(key: Option[String]): String = {
    _ match {
      case Some(x) => redisType + delimiter + prefix + delimiter + key
      case None => throw new IllegalArgumentException
    }
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
}

/**
 * Redisの文字列型に関する設定やオペレーションを持つtrait
 * @tparam T キャッシュとして格納する値の型
 */
trait RedisStringsOperator[T] extends RedisCommonOperator[T] {
  final val typePrefix = "STRINGS"

  override def setupCacheKey(key: String): String = {
    typePrefix + delimiter + prefix + delimiter + key
  }

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
}
