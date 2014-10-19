package example.cache

import com.redis.RedisClient
import org.specs2.ScalaCheck
import org.specs2.execute.Results
import org.specs2.mutable.Specification

/**
 * Created by a12043 on 2014/10/18.
 */
class RedisListsOperatorSpec extends Specification with Results with ScalaCheck {
  val c = new RedisClient("localhost", 6379)

  step {
    val flushed = c.flushall
    flushed must beTrue
  }

  val list = List(
    new TestObj(100, "element1"),
    new TestObj(200, "element2"),
    new TestObj(300, "element3")
  )

  "get" should {
    "キャッシュに値が入ってないのでNoneが返る" in {
      val result = TestListsCache1.get(0)
      result must beSome[List[TestObj]]
      result.get.size must be_==(0)
    }

    "db1に\"OK\"をセット。db1からはとれるが2からは取れない。またプレフィックス違いのキャッシュからも取れない" in {
      val key = 1

      // db1のキャッシュに値をセットしてから
      val set = TestListsCache1.set(1, list)
      set must beSome[Long]
      set.get must be_==(list.size)

      // 値が取れる
      val result1 = TestListsCache1.get(key)
      result1 must beSome[List[TestObj]]
      result1.get must be_===(list)

      // dbが違うから値が取れない
      val result2 = TestListsCache2.get(key)
      result2 must beSome[List[TestObj]]
      result2.get must beEmpty

      // prefixが違うから値が取れない
      val result3 = TestListsCache3.get(key)
      result3 must beSome[List[TestObj]]
      result3.get must beEmpty
    }

    "有効期限が過ぎると取れなくなる" in {
      // db1のキャッシュに値をセットしてから
      val set = TestListsCache1.set(2, list)
      set must beSome(list.size)

      // 有効期限分の時間プラスちょっとだけ待つ
      Thread.sleep(TestListsCache1.expireSec.get * 1001L)

      val result = TestListsCache1.get(2)
      result must beSome[List[TestObj]]
      result.get must beEmpty
    }
  }

  step {
    val flushed = c.flushall
    flushed must beTrue
  }

  "set" should {
    "trueが返る" in {
      val result = TestListsCache1.set(10, list)

      result must beSome(list.size)
    }
  }
}

class TestObj(val id: Int, val name: String) {
  override def equals(other: Any): Boolean = {
    other match {

      case that: TestObj =>
        (that canEqual this) &&
          id == that.id &&
          name == that.name

      case _ => false
    }
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[TestObj]

  override def hashCode = id.hashCode + name.hashCode
}

object TestListsCache1 extends RedisListsOperator[Int, TestObj] {
  override val dbNumber = 0
  override val valueType = classOf[TestObj]
  override val expireSec = Option(5)
  override val prefix = "hoge"
}

/**
 * TestStringsCache1とdb番号だけが違う
 */
object TestListsCache2 extends RedisListsOperator[Int, TestObj] {
  override val dbNumber = 1
  override val valueType = classOf[TestObj]
  override val expireSec = Option(5)
  override val prefix = "hoge"
}

/**
 * TestStringsCache1とプレフィックスが違う
 */
object TestListsCache3 extends RedisListsOperator [Int, TestObj] {
  override val dbNumber = 0
  override val valueType = classOf[TestObj]
  override val expireSec = Option(5)
  override val prefix = "fuga"
}
