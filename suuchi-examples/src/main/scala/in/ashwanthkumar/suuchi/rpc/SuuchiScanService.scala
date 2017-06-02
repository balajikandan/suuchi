package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.examples.rpc.generated.SuuchiRPC.{ScanRequest, ScanResponse}
import in.ashwanthkumar.suuchi.examples.rpc.generated.{ScanGrpc, SuuchiRPC}
import in.ashwanthkumar.suuchi.partitioner.SuuchiHash
import in.ashwanthkumar.suuchi.store.{KV, Store}
import in.ashwanthkumar.suuchi.utils.ByteArrayUtils
import io.grpc.stub.{ServerCallStreamObserver, StreamObserver}

class SuuchiScanService(store: Store) extends ScanGrpc.ScanImplBase {

  override def scan(request: ScanRequest, responseObserver: StreamObserver[ScanResponse]): Unit = {
    val observer = responseObserver.asInstanceOf[ServerCallStreamObserver[ScanResponse]]
    val start    = request.getStart
    val end      = request.getEnd

    val scanner = store.scanner()
    scanner.prepare()
    val it = scanner
      .scan()
      .filter(kv => ByteArrayUtils.isHashKeyWithinRange(start, end, kv.key, SuuchiHash))
      .map(buildResponse)

    observer.setOnCancelHandler(new Runnable() {
      override def run() = {
        scanner.close()
      }
    })
    observer.setOnReadyHandler(new Runnable() {
      override def run() = {
        while (observer.isReady && it.hasNext) {
          observer.onNext(it.next)
        }

        if (!it.hasNext) {
          observer.onCompleted()
          scanner.close()
        }
      }
    })
  }

  private def buildKV(kv: KV) = {
    SuuchiRPC.KV
      .newBuilder()
      .setKey(ByteString.copyFrom(kv.key))
      .setValue(ByteString.copyFrom(kv.value))
      .build()
  }

  private def buildResponse(response: KV): ScanResponse = {
    SuuchiRPC.ScanResponse
      .newBuilder()
      .setKv(buildKV(response))
      .build()
  }
}
