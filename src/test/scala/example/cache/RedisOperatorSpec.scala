package example.cache

import org.specs2.ScalaCheck
import org.specs2.execute.Results
import org.specs2.mutable.Specification

/**
 * Created by a12043 on 2014/10/18.
 */
class RedisOperatorSpec extends Specification with Results with ScalaCheck {
  "get" should {
    "キャッシュに値が入ってないのでNoneが返る" in {
      val result = TestStringsCache1.get(0)

      result must beNone
    }

    "db1に\"OK\"をセット。db1からはとれるが2からは取れない。またプレフィックス違いのキャッシュからも取れない" in {
      // db1のキャッシュに値をセットしてから
      val set = TestStringsCache1.set(1, "OK")
      set must beTrue

      val result1 = TestStringsCache1.get(1)
      result1 must beSome("OK")

      val result2 = TestStringsCache2.get(1)
      result2 must beNone

      val result3 = TestStringsCache3.get(1)
      result3 must beNone
    }

    "有効期限が過ぎると取れなくなる" in {
      // db1のキャッシュに値をセットしてから
      val set = TestStringsCache1.set(1, "OK")
      set must beTrue

      // 有効期限分の時間プラスちょっとだけ待つ
      Thread.sleep(TestStringsCache1.expireSec.get * 1001L)

      val result = TestStringsCache1.get(1)
      result must beNone
    }
  }

  "set" should {
    "trueが返る" in {
      val result = TestStringsCache1.set(10, "testtest")

      result must be_==(true)
    }
  }
}

object TestStringsCache1 extends RedisStringsOperator[Int, String] {
  override val dbNumber = 1
  override val valueType = classOf[String]
  override val expireSec = Option(5)
  override val prefix = "hoge"
}

/**
 * TestStringsCache1とdb番号だけが違う
 */
object TestStringsCache2 extends RedisStringsOperator[Int, String] {
  override val dbNumber = 2
  override val valueType = classOf[String]
  override val expireSec = Option(5)
  override val prefix = "hoge"
}

/**
 * TestStringsCache1とプレフィックスが違う
 */
object TestStringsCache3 extends RedisStringsOperator [Int, String] {
  override val dbNumber = 1
  override val valueType = classOf[String]
  override val expireSec = Option(5)
  override val prefix = "fuga"
}
