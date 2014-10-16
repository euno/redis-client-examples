package example.cache.operation

import com.google.api.ads.dfp.axis.v201405.LineItem
import example.cache.operation.StringsRedisCache.{FugaCache, HogeCache}

/**
 * Created by a12043 on 2014/10/16.
 */
object RedisMain {

  def main(args: Array[String]) = {
    HogeCache.set("hogehoge", "hogeValue");
    println(HogeCache.get("hogehoge"));
    println(HogeCache.get("hogehogehoge"));

    val item = new LineItem()
    item.setId(1234L)
    item.setName("らいんあいてむのテスト")

    FugaCache.set("test", item)
    println(FugaCache.get("test"))

    FugaCache.set(item)
    println(FugaCache.get(item.getId.toString))

    var del = HogeCache.delete("hogehoge")
    println(s"deleted count $del")

    del = HogeCache.delete("hogehoge", "test")
    println(s"deleted count $del")

    del =FugaCache.delete("test", item.getId.toString)
    println(s"deleted count $del")
  }
}

object StringsRedisCache {

  object HogeCache extends RedisStringsOperator[String] {
    override val typeDef = classOf[String]
    override val dbNumber: Int = 1
    override val prefix: String = "hoge"
  }

  object FugaCache extends RedisStringsOperator[LineItem] {
    override val typeDef = classOf[LineItem]
    override val dbNumber: Int = 2
    override val prefix: String = "lineItem"
    override def keyOf(value: LineItem): String = value.getId.toString
  }
}