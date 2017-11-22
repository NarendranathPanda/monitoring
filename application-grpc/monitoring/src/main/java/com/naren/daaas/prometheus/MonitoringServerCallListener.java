package com.naren.daaas.prometheus;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;

/**
 * A {@link ForwardingServerCallListener} which updates Prometheus metrics for a
 * single rpc based on updates received from grpc.
 */
class MonitoringServerCallListener<R> extends ForwardingServerCallListener<R> {
	private final ServerCall.Listener<R> delegate;
	private final GrpcMethod grpcMethod;
	private final ServerMetrics serverMetrics;

	MonitoringServerCallListener(ServerCall.Listener<R> delegate, ServerMetrics serverMetrics, GrpcMethod grpcMethod) {
		this.delegate = delegate;
		this.serverMetrics = serverMetrics;
		this.grpcMethod = grpcMethod;
	}

	@Override
	protected ServerCall.Listener<R> delegate() {
		return delegate;
	}

	@Override
	public void onMessage(R request) {
		recordStreamMessageReceived();
		super.onMessage(request);
	}

	@Override
	public void onCancel() {
		recordStreamMessageReceived();
		super.onCancel();
	}

	@Override
	public void onComplete() {
		recordStreamMessageReceived();
		super.onComplete();
	}

	@Override
	public void onHalfClose() {
		recordStreamMessageReceived();
		super.onHalfClose();
	}

	@Override
	public void onReady() {
		recordStreamMessageReceived();
		super.onReady();
	}

	private void recordStreamMessageReceived() {
		serverMetrics.recordStreamMessageReceived();
	}
}