package example.cache

import com.redis.RedisClient
import org.specs2.ScalaCheck
import org.specs2.execute.Results
import org.specs2.mutable.Specification

/**
 * Created by a12043 on 2014/10/18.
 */
class RedisStringsOperatorSpec extends Specification with Results with ScalaCheck {
  val c = new RedisClient("localhost", 6379)

  step {
    val flushed = c.flushall
    flushed must beTrue
  }

  "get" should {
    "キャッシュに値が入ってないのでNoneが返る" in {
      val result = TestStringsCache1.get(0)
      result must beNone
    }

    "db1に\"OK\"をセット。db1からはとれるが2からは取れない。またプレフィックス違いのキャッシュからも取れない" in {
      val key = 1
      val name = "testOK"
      val value = new TestObj(key, name)

      // db1のキャッシュに値をセットしてから
      val set = TestStringsCache1.set(key, value)
      set must beTrue

      // 値が取れる
      val result1 = TestStringsCache1.get(key)
      result1 must beSome(value)

      // dbが違うから値が取れない
      val result2 = TestStringsCache2.get(key)
      result2 must beNone

      // prefixが違うから値が取れない
      val result3 = TestStringsCache3.get(key)
      result3 must beNone
    }

    "有効期限が過ぎると取れなくなる" in {
      // db1のキャッシュに値をセットしてから
      val set = TestStringsCache1.set(1, new TestObj(1, "testOK"))
      set must beTrue

      // 有効期限分の時間プラスちょっとだけ待つ
      Thread.sleep(TestStringsCache1.expireSec.get * 1001L)

      val result = TestStringsCache1.get(1)
      result must beNone
    }
  }

  "set" should {
    "trueが返る" in {
      val result = TestStringsCache1.set(10, new TestObj(10, "testSetOK"))

      result must beTrue
    }
  }
}

object TestStringsCache1 extends RedisStringsOperator[Int, TestObj] {
  override val dbNumber = 0
  override val valueType = classOf[TestObj]
  override val expireSec = Option(5)
  override val prefix = "hoge"
}

/**
 * TestStringsCache1とdb番号だけが違う
 */
object TestStringsCache2 extends RedisStringsOperator[Int, TestObj] {
  override val dbNumber = 1
  override val valueType = classOf[TestObj]
  override val expireSec = Option(5)
  override val prefix = "hoge"
}

/**
 * TestStringsCache1とプレフィックスが違う
 */
object TestStringsCache3 extends RedisStringsOperator [Int, TestObj] {
  override val dbNumber = 0
  override val valueType = classOf[TestObj]
  override val expireSec = Option(5)
  override val prefix = "fuga"
}
