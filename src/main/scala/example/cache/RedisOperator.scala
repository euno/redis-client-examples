package example.cache

import com.redis.serialization.{Format, Parse}
import com.redis.{RedisClient, RedisClientPool}

/**
 * Redisのキャッシュに関する操作機能を持つ最も基本的なtrait
 * @tparam K Redisのキーとなる型
 * @tparam V Redisの値となる型
 */
sealed trait RedisOperator[K, V] {
  final val delimiter = "#"

  final val redisClientPool: RedisClientPool = new RedisClientPool("localhost", 6379)

  val dbNumber: Int

  val prefix: String

  val expireSec: Option[Int] = None

  val redisType: String

  val valueType: Class[V]

  /**
   * キーや値のセリアライズに使うFormat
   */
  implicit val format = new Format(RedisCacheMapper.format)

  /**
   * キーや値のデシリアライズに使うParse(typeDefに依存するためlazy)
   */
  implicit lazy val parse = new Parse[V](RedisCacheMapper.parse[V](_: Array[Byte], valueType))

  /**
   * Redisに実際に格納するキーを組み立てる関数
   * @param key アプリケーションから渡されるキー
   * @return 実際にRedisに保存されるキー
   */
  def setupCacheKey(key: K): String = {
    if (key == null) {
      throw new IllegalArgumentException("key string is not acceptable null.")
    }

    val keyString = key.toString
    if (keyString.isEmpty) {
      throw new IllegalArgumentException("key string is not acceptable empty.")
    }

    redisType + delimiter + prefix + delimiter + keyString
  }

  /**
   * 複数のキーが渡された時に実際にRedisに格納するキーに変換して返す関数
   * @param keys
   * @return
   */
  def setupCacheKeys(keys: Seq[K]): Seq[String] = {

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
  def delete(key: K, keys: K*): Option[Long] = {
    val cacheKey = setupCacheKey(key)
    if (keys == null || keys.isEmpty) {
      call[Option[Long]](_.del(cacheKey))
    } else {
      call[Option[Long]](_.del(cacheKey, setupCacheKeys(keys): _*))
    }
  }

  /**
   * 指定されたキーのエントリーが存在するかどうかをチェックします
   * @param key 存在するか確認するキー
   * @return 存在していればtrue
   */
  def exists(key: K): Boolean = {
    call[Boolean](_.exists(setupCacheKey(key)))
  }

  /**
   * 指定されたキーに対して、指定された秒数の有効期限を新たに設定します。
   * すでに有効期限が設定されている場合も、指定された秒数で新たに上書きされます
   * @param key 有効期限を指定するキー
   * @param expire 有効期限の秒数。デフォルトはexpireSecで指定された秒数
   * @return 有効期限が設定されたtrue
   */
  def setExpire(key: K, expire: Option[Int] = expireSec): Boolean = {
    expire match {
      case Some(x) => call[Boolean](_.expire(setupCacheKey(key), x))
      case None => false
    }
  }
}

/**
 * 特定の型とは関係なくRedisサーバに対するオペレーションを実行できるtrait
 */
trait RedisServerOperator extends RedisOperator[Any, Any] {
  override final val prefix = ""
  override final val redisType = ""
  override final val valueType = classOf[Any]

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
* @tparam K キャッシュの際に利用するキーの型
* @tparam V キャッシュとして格納する値の型
*/
trait RedisStringsOperator[K, V] extends RedisOperator[K, V] {
  override val redisType = "STRINGS"

  /**
   * 指定されたキーに対応する値を取得します
   * @param key 取得する値のキー
   * @return 取得した値
   */
  def get(key: K): Option[V] = {
    call[Option[V]](_.get[V](setupCacheKey(key)))
  }

  /**
   *
   * @param key
   * @param orElse
   * @return
   */
  def getOrElse(key: K)(orElse: => V)(onlySet: Boolean = false): Option[V] = {
    if (onlySet) {
      // キャッシュを読まず、結果を載せたいだけの場合
      return setAndGetOption(key, orElse)
    }

    val cacheValue = get(key)

    cacheValue match {
      case Some(x) => cacheValue  // キャッシュにヒットした場合
      case None => {
        // キャッシュにヒットしなかった場合
        setAndGetOption(key, orElse)
      }
    }
  }

  /**
   * 指定されたキーに対して、指定された値を格納します。
   * @param key 格納する値のキー
   * @param value 格納する値
   * @return
   */
  def set(key: K, value: V): Boolean = {
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

  private def setAndGetOption(key: K, orElse: V): Option[V] = {
    set(key, orElse)
    Option(orElse)
  }
}

/**
 * Redisのリスト型に関する設定やオペレーションを持つtrait
 * @tparam K キャッシュの際に利用するキーの型
 * @tparam V 値のリストに格納される各要素の型
 */
trait RedisListsOperator[K, V] extends RedisOperator[K, V] {
  final val bulkSetUnit = 10
  override val redisType = "LISTS"

  /**
   * 指定されたキーで、指定されたvalueの内容を新たにリスト型の値として登録します
   * @param key
   * @param value
   * @return
   */
  def set(key: K, value: List[V]): Option[Long] = {
    val cacheKey = setupCacheKey(key)

    call[Option[Long]](
      client => {
        // 既に値がある場合は一度クリアする
        client.del(cacheKey)

        // 値のセット
        val result = client.rpush(cacheKey, value(0), value.slice(1, value.size): _*)

        // 有効期限の設定がある場合はそのセット
        expireSec match {
          case Some(x) => client.expire(cacheKey, x)
          case None =>
        }

        return result
      }
    )
  }

  /**
   * 指定されたキーの内容をListとして取得します
   * @param key 取得するキー
   * @return そのキーに対応するすべての要素を含むList
   */
  def get(key: K): Option[List[V]] = {
    val value = call[Option[List[Option[V]]]](_.lrange(setupCacheKey(key), 0, -1))
    value match {
      case Some(x) => Option(x.flatten)
      case None => None
    }
  }

  /**
   * 指定されたキーの内容をListとして取得します
   * もしRedis内に指定されたキーがなかった場合は、orElseの内容をキャッシュに格納し、
   * orElseの内容をOptionにくるんで返します
   * @param key 取得するキー
   * @param orElse 取得できなかった場合に実行する関数
   * @return そのキーに対応するすべての要素を含むList
   */
  def getOrElse(key: K)(orElse: => List[V])(onlySet: Boolean = false): Option[List[V]] = {
    if (onlySet) {
      return setAndGetOption(key, orElse)
    }

    val cacheValue = get(key)

    cacheValue match {
      case Some(x) => cacheValue  // キャッシュにヒットした場合
      case None => {
        // キャッシュにヒットしなかった場合
        setAndGetOption(key, orElse)
      }
    }
  }

  private def setAndGetOption(key: K, orElse: List[V]): Option[List[V]] = {
    set(key, orElse)
    Option(orElse)
  }
}
