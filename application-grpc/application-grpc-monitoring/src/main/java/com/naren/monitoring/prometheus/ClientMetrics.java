package com.naren.monitoring.prometheus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.grpc.MethodDescriptor;
import io.grpc.Status.Code;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleCollector;

/**
 * Prometheus metric definitions used for client-side monitoring of grpc
 * services.
 */
class ClientMetrics {
	private static final String CLIENT = "client";

	private static final String GRPC = "grpc";

	private static final String GRPC_METHOD = "grpc_method";

	private static final String GRPC_SERVICE = "grpc_service";

	private static final String GRPC_TYPE = "grpc_type";

	private static final Counter.Builder rpcStartedBuilder = Counter.build().namespace(GRPC).subsystem(CLIENT)
			.name("started_total").labelNames(GRPC_TYPE, GRPC_SERVICE, GRPC_METHOD)
			.help("Total number of RPCs started on the client .");

	private static final Counter.Builder rpcCompletedBuilder = Counter.build().namespace(GRPC).subsystem(CLIENT)
			.name("completed").labelNames(GRPC_TYPE, GRPC_SERVICE, GRPC_METHOD, "code")
			.help("Total number of RPCs completed on the client, regardless of success or failure .");

	private static final Histogram.Builder completedLatencySecondsBuilder = Histogram.build().namespace(GRPC)
			.subsystem(CLIENT).name("completed_latency_seconds").labelNames(GRPC_TYPE, GRPC_SERVICE, GRPC_METHOD)
			.help("Histogram of rpc response latency (in seconds) for completed rpcs .");

	private static final Counter.Builder streamMessagesReceivedBuilder = Counter.build().namespace(GRPC)
			.subsystem(CLIENT).name("msg_received_total").labelNames(GRPC_TYPE, GRPC_SERVICE, GRPC_METHOD)
			.help("Total number of stream messages received from the server .");

	private static final Counter.Builder streamMessagesSentBuilder = Counter.build().namespace(GRPC).subsystem(CLIENT)
			.name("msg_sent_total").labelNames(GRPC_TYPE, GRPC_SERVICE, GRPC_METHOD)
			.help("Total number of stream messages sent by the client .");

	private final Counter rpcStarted;
	private final Counter rpcCompleted;
	private final Counter streamMessagesReceived;
	private final Counter streamMessagesSent;
	private final Optional<Histogram> completedLatencySeconds;

	private final GrpcMethod method;

	private ClientMetrics(GrpcMethod method, Counter rpcStarted, Counter rpcCompleted, Counter streamMessagesReceived,
			Counter streamMessagesSent, Optional<Histogram> completedLatencySeconds) {
		this.method = method;
		this.rpcStarted = rpcStarted;
		this.rpcCompleted = rpcCompleted;
		this.streamMessagesReceived = streamMessagesReceived;
		this.streamMessagesSent = streamMessagesSent;
		this.completedLatencySeconds = completedLatencySeconds;
	}

	public void recordCallStarted() {
		addLabels(rpcStarted).inc();
	}

	public void recordClientHandled(Code code) {
		addLabels(rpcCompleted, code.toString()).inc();
	}

	public void recordStreamMessageSent() {
		addLabels(streamMessagesSent).inc();
	}

	public void recordStreamMessageReceived() {
		addLabels(streamMessagesReceived).inc();
	}

	/**
	 * Only has any effect if monitoring is configured to include latency
	 * histograms. Otherwise, this does nothing.
	 */
	public void recordLatency(double latencySec) {
		if (!completedLatencySeconds.isPresent()) {
			return;
		}
		addLabels(completedLatencySeconds.get()).observe(latencySec);
	}

	/**
	 * Knows how to produce {@link ClientMetrics} instances for individual methods.
	 */
	static class Factory {
		private final Counter rpcStarted;
		private final Counter rpcCompleted;
		private final Counter streamMessagesReceived;
		private final Counter streamMessagesSent;
		private final Optional<Histogram> completedLatencySeconds;

		Factory(Configuration configuration) {
			CollectorRegistry registry = configuration.getCollectorRegistry();
			this.rpcStarted = rpcStartedBuilder.register(registry);
			this.rpcCompleted = rpcCompletedBuilder.register(registry);
			this.streamMessagesReceived = streamMessagesReceivedBuilder.register(registry);
			this.streamMessagesSent = streamMessagesSentBuilder.register(registry);

			if (configuration.isIncludeLatencyHistograms()) {
				this.completedLatencySeconds = Optional.of(ClientMetrics.completedLatencySecondsBuilder
						.buckets(configuration.getLatencyBuckets()).register(registry));
			} else {
				this.completedLatencySeconds = Optional.empty();
			}
		}

		/** Creates a {@link ClientMetrics} for the supplied method. */
		<R, S> ClientMetrics createMetricsForMethod(MethodDescriptor<R, S> methodDescriptor) {
			return new ClientMetrics(GrpcMethod.of(methodDescriptor), rpcStarted, rpcCompleted, streamMessagesReceived,
					streamMessagesSent, completedLatencySeconds);
		}
	}

	private <T> T addLabels(SimpleCollector<T> collector, String... labels) {
		List<String> allLabels = new ArrayList<>();
		Collections.addAll(allLabels, method.type(), method.serviceName(), method.methodName());
		Collections.addAll(allLabels, labels);
		return collector.labels(allLabels.toArray(new String[0]));
	}
}
