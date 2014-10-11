package example.scala_redis

import com.redis.{RedisClientPool, RedisClient}

/**
 * scala-redis
 */
object ScalaRedisExample {

  def connect(): RedisClient = {
    new RedisClient("localhost", 6379)
  }

  def stringTest(client: RedisClient): Unit = {
    // お掃除
    client.flushall

    // 文字列をセット
    client.set("strkey", "hoge")
    val str = client.get("strkey")

    // 入れた文字列が取得できる
    println(str)

    // オブジェクト(Map)をセット
    client.set("mapkey", Map("foo" -> "var", "hoge" -> "fuga"))
    val map = client.get("mapkey")

    // 文字列にシリアライズされた情報が保存されており、そのまま取得できる
    println(map)
  }

  def listTest(client: RedisClient): Unit = {
    // お掃除
    client.flushall

    // listkeyというリスト型のキーに、値を4つ右に追加していく
    client.rpush("listkey", "rpush1", "rpush2", "rpush3", "rpush4")

    // 長さ4のList型の値が入った
    println(client.llen("listkey"))

    // すべて取り出すと入れた順に出てくる
    println(client.lrange("listkey", 0, -1))

    // listkeyに値を5つ左から追加していく
    client.lpush("listkey", "lpush1", "lpush2", "lpush3", "lpush4", "lpush5")

    // 長さが9になった
    println(client.llen("listkey"))

    // すべて取り出すと、後から追加した5つは左に追加されたことがわかる
    println(client.lrange("listkey", 0, -1))

    // 範囲指定で取ってみる
    println(client.lrange("listkey", 3, 7))
  }


  def pooling(): Unit = {
    val pool = new RedisClientPool("localhost", 6379)
  }

  def main(args: Array[String]) : Unit = {
    val c = connect()
    stringTest(c)
    listTest(c)
  }
}
