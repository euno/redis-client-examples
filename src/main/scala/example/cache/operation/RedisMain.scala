package example.cache.operation

import com.google.api.ads.dfp.axis.v201405.LineItem


/**
 * Created by a12043 on 2014/10/16.
 */
object RedisMain {

  def main(args: Array[String]) = {
    /*
    HogeCache.set("hogehoge", "hogeValue");
    println(HogeCache.get("hogehoge"));
    println(HogeCache.get("hogehogehoge"));

    val item = new LineItem()
    item.setId(1234L)
    item.setName("らいんあいてむのテスト")

    FugaCache.set(100L, item)
    println(FugaCache.get(100L))

    FugaCache.set(item.getId, item)
    println(FugaCache.get(item.getId))

    var del = HogeCache.delete("hogehoge")
    println(s"deleted count $del")

    del = HogeCache.delete("hogehoge", "test")
    println(s"deleted count $del")

    del =FugaCache.delete(123L, item.getId)
    println(s"deleted count $del")

    val listValue = List("test1", "test2", "test3")
    FooCache.set("test", listValue)

    println(FooCache.getAll("test"))

    val item2 = new LineItem()
    item2.setId(1111L)
    item2.setName("あいてむ2")

    val item3 = new LineItem()
    item3.setId(2222L)
    item3.setName("あいてむ3")

    val listValue2 = List(item, item2, item3, item, item2, item3, item, item2, item3, item, item2, item3)
    BaaCache.set(9999L, listValue2)

    println(BaaCache.getAll(9999L))

    println(BaaCache.getRange(9999L, 1, 2))
*/
    val tupleValue1 = (111L, "testTuple1")
    val tupleValue2 = (222L, "testTuple2")
    val tupleValue3 = (333L, "testTuple3")

    val tuples = List(tupleValue1, tupleValue2, tupleValue3)
    TupleCache.set("tupleTest", tuples)

    println(TupleCache.getAll("tupleTest"))
  }
}

// 文字列型のキャッシュ定義
sealed abstract class StringsRedisCache[K, V] extends RedisStringsOperator[K, V] {
  override val dbNumber = 1
}

case object HogeCache extends StringsRedisCache[String, String] {
  override val valueType = classOf[String]
  override val prefix = "hoge"
}

case object FugaCache extends StringsRedisCache[Long, LineItem] {
  override val valueType = classOf[LineItem]
  override val prefix = "lineItem"
}


// リスト型のキャッシュ定義
sealed abstract class ListsRedisCache[K, V] extends RedisListsOperator[K, V] {
  override val dbNumber = 2
}

case object FooCache extends ListsRedisCache[String, String] {
  override val valueType = classOf[String]
  override val prefix = "foo"
}

case object BaaCache extends ListsRedisCache[Long, LineItem] {
  override val valueType = classOf[LineItem]
  override val prefix = "baa"
}

case object TupleCache extends ListsRedisCache[String, (Long, String)] {
  override val valueType = classOf[(Long, String)]
  override val prefix = "tuple"
}