package in.ashwanthkumar.suuchi.membership.scalecube

import in.ashwanthkumar.suuchi.membership.{SeedProvider, MemberAddress, MemberListener, Membership}
import io.scalecube.cluster.gossip.GossipConfig
import io.scalecube.cluster.membership.{MembershipConfig, MembershipEvent}
import io.scalecube.cluster.{Cluster, ClusterConfig, ICluster}
import io.scalecube.transport.{Address, TransportConfig}
import org.slf4j.LoggerFactory
import rx.lang.scala.ImplicitFunctionConversions._

import scala.collection.JavaConversions._
import scala.language.implicitConversions

class ScaleCubeMembership(port: Int, gossipConfig: Option[GossipConfig] = None, listeners: List[MemberListener]) extends Membership(listeners) {
  protected val log = LoggerFactory.getLogger(getClass)

  var cluster: ICluster = _

  override def start(seedProvider: SeedProvider): Membership = {
    val clusterConfig = ClusterConfig.builder()
      .transportConfig(
        TransportConfig.builder()
          .port(port).build()
      )
    gossipConfig.foreach(clusterConfig.gossipConfig)
    if (seedProvider.nodes.isEmpty) {
      cluster = Cluster.joinAwait(clusterConfig.build())
    } else {
      val seedNodes = seedProvider.nodes.map(m => Address.create(m.host, m.port))
      cluster = Cluster.joinAwait(
        clusterConfig
          .membershipConfig(MembershipConfig.builder.seedMembers(seedNodes).build)
          .build()
      )
    }
    cluster.listenMembership()
      .filter({ m: MembershipEvent => m.isAdded })
      .map[MemberAddress]({ m: MembershipEvent => MemberAddress(m.member().address().toString) })
      .subscribe(this.onJoin)

    cluster.listenMembership()
      .filter({ m: MembershipEvent => m.isRemoved })
      .map[MemberAddress]({ m: MembershipEvent => MemberAddress(m.member().address().toString) })
      .subscribe(this.onLeave)
    this
  }
  override def stop(): Unit = cluster.shutdown().get()
  override def nodes: Iterable[MemberAddress] = cluster.members().map(m => MemberAddress(m.address().toString))
  override def whoami: MemberAddress = MemberAddress(cluster.address().toString)
}
