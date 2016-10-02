package in.ashwanthkumar.suuchi.partitioner

import in.ashwanthkumar.suuchi.membership.MemberAddress
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

object IdentityHash extends Hash {
  override def hash(bytes: Array[Byte]): Integer = bytes.toString.toInt
}

class ConsistentHashRingSpec extends FlatSpec {
  "ConsistentHashRing" should "pin nodes into the ring accounting for virtual nodes" in {
    val ring = new ConsistentHashRing(SuuchiHash, 3)
    ring.init(List(MemberAddress("host1", 1), MemberAddress("host2", 2), MemberAddress("host3", 3)))

    ring.nodes.size() should be(9)

    ring.add(MemberAddress("host100", 100))
    ring.nodes.size() should be(12)
  }

  it should "remove nodes & its replica nodes on remove" in {
    val ring = new ConsistentHashRing(SuuchiHash, 3)
    ring.init(List(MemberAddress("host1", 1), MemberAddress("host2", 2), MemberAddress("host3", 3)))

    ring.remove(MemberAddress("host1", 1))
    ring.nodes.size should be(6)

    ring.remove(MemberAddress("host2", 2))
    ring.nodes.size should be(3)

    ring.remove(MemberAddress("host3", 3))
    ring.nodes.size should be(0)

    ring.remove(MemberAddress("host4", 4))
    ring.nodes.size should be(0)
  }
}
