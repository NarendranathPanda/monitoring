package com.naren.sample.grpc.server.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.naren.monitoring.prometheus.GrpcMetricService;
import com.naren.sample.grpc.api.GreeterGrpc;
import com.naren.sample.grpc.api.HelloReply;
import com.naren.sample.grpc.api.HelloRequest;
import com.naren.sample.grpc.api.MetricReply;
import com.naren.sample.grpc.api.MetricRequest;

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;

public class GreeterService extends GreeterGrpc.AbstractGreeter implements BindableService {

	private static final Logger LOG = LoggerFactory.getLogger(GreeterService.class);

	@Override
	public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
		LOG.info("sayHello endpoint received request from " + request.getName());
		HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
		responseObserver.onNext(reply);
		responseObserver.onCompleted();
	}

	@Override
	public void metric(MetricRequest request, StreamObserver<MetricReply> responseObserver) {

		LOG.info("metric endpoint received request from " + request.getName());
		MetricReply reply = MetricReply.newBuilder().setMetric(GrpcMetricService.metrics()).build();
		responseObserver.onNext(reply);
		responseObserver.onCompleted();

	}


}
