package io.notifyhub.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * NotifyHub —— 多语言通知推送服务。
 * 本文件是唯一契约：服务端将来重写（Go/Rust/Zig）时不得变更既有字段的编号与语义。
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class NotifyGrpc {

  private NotifyGrpc() {}

  public static final java.lang.String SERVICE_NAME = "notify.v1.Notify";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.notifyhub.v1.PublishRequest,
      io.notifyhub.v1.PublishAck> getPublishMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Publish",
      requestType = io.notifyhub.v1.PublishRequest.class,
      responseType = io.notifyhub.v1.PublishAck.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.notifyhub.v1.PublishRequest,
      io.notifyhub.v1.PublishAck> getPublishMethod() {
    io.grpc.MethodDescriptor<io.notifyhub.v1.PublishRequest, io.notifyhub.v1.PublishAck> getPublishMethod;
    if ((getPublishMethod = NotifyGrpc.getPublishMethod) == null) {
      synchronized (NotifyGrpc.class) {
        if ((getPublishMethod = NotifyGrpc.getPublishMethod) == null) {
          NotifyGrpc.getPublishMethod = getPublishMethod =
              io.grpc.MethodDescriptor.<io.notifyhub.v1.PublishRequest, io.notifyhub.v1.PublishAck>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Publish"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.PublishRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.PublishAck.getDefaultInstance()))
              .setSchemaDescriptor(new NotifyMethodDescriptorSupplier("Publish"))
              .build();
        }
      }
    }
    return getPublishMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.notifyhub.v1.SubscribeRequest,
      io.notifyhub.v1.Event> getSubscribeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Subscribe",
      requestType = io.notifyhub.v1.SubscribeRequest.class,
      responseType = io.notifyhub.v1.Event.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<io.notifyhub.v1.SubscribeRequest,
      io.notifyhub.v1.Event> getSubscribeMethod() {
    io.grpc.MethodDescriptor<io.notifyhub.v1.SubscribeRequest, io.notifyhub.v1.Event> getSubscribeMethod;
    if ((getSubscribeMethod = NotifyGrpc.getSubscribeMethod) == null) {
      synchronized (NotifyGrpc.class) {
        if ((getSubscribeMethod = NotifyGrpc.getSubscribeMethod) == null) {
          NotifyGrpc.getSubscribeMethod = getSubscribeMethod =
              io.grpc.MethodDescriptor.<io.notifyhub.v1.SubscribeRequest, io.notifyhub.v1.Event>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Subscribe"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.SubscribeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.Event.getDefaultInstance()))
              .setSchemaDescriptor(new NotifyMethodDescriptorSupplier("Subscribe"))
              .build();
        }
      }
    }
    return getSubscribeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.notifyhub.v1.PublishRequest,
      io.notifyhub.v1.PublishAck> getPublishStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PublishStream",
      requestType = io.notifyhub.v1.PublishRequest.class,
      responseType = io.notifyhub.v1.PublishAck.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<io.notifyhub.v1.PublishRequest,
      io.notifyhub.v1.PublishAck> getPublishStreamMethod() {
    io.grpc.MethodDescriptor<io.notifyhub.v1.PublishRequest, io.notifyhub.v1.PublishAck> getPublishStreamMethod;
    if ((getPublishStreamMethod = NotifyGrpc.getPublishStreamMethod) == null) {
      synchronized (NotifyGrpc.class) {
        if ((getPublishStreamMethod = NotifyGrpc.getPublishStreamMethod) == null) {
          NotifyGrpc.getPublishStreamMethod = getPublishStreamMethod =
              io.grpc.MethodDescriptor.<io.notifyhub.v1.PublishRequest, io.notifyhub.v1.PublishAck>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PublishStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.PublishRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.PublishAck.getDefaultInstance()))
              .setSchemaDescriptor(new NotifyMethodDescriptorSupplier("PublishStream"))
              .build();
        }
      }
    }
    return getPublishStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.notifyhub.v1.PlatformConfig,
      io.notifyhub.v1.PlatformConfig> getUpsertPlatformMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpsertPlatform",
      requestType = io.notifyhub.v1.PlatformConfig.class,
      responseType = io.notifyhub.v1.PlatformConfig.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.notifyhub.v1.PlatformConfig,
      io.notifyhub.v1.PlatformConfig> getUpsertPlatformMethod() {
    io.grpc.MethodDescriptor<io.notifyhub.v1.PlatformConfig, io.notifyhub.v1.PlatformConfig> getUpsertPlatformMethod;
    if ((getUpsertPlatformMethod = NotifyGrpc.getUpsertPlatformMethod) == null) {
      synchronized (NotifyGrpc.class) {
        if ((getUpsertPlatformMethod = NotifyGrpc.getUpsertPlatformMethod) == null) {
          NotifyGrpc.getUpsertPlatformMethod = getUpsertPlatformMethod =
              io.grpc.MethodDescriptor.<io.notifyhub.v1.PlatformConfig, io.notifyhub.v1.PlatformConfig>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpsertPlatform"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.PlatformConfig.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.PlatformConfig.getDefaultInstance()))
              .setSchemaDescriptor(new NotifyMethodDescriptorSupplier("UpsertPlatform"))
              .build();
        }
      }
    }
    return getUpsertPlatformMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.notifyhub.v1.Empty,
      io.notifyhub.v1.PlatformList> getListPlatformsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListPlatforms",
      requestType = io.notifyhub.v1.Empty.class,
      responseType = io.notifyhub.v1.PlatformList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.notifyhub.v1.Empty,
      io.notifyhub.v1.PlatformList> getListPlatformsMethod() {
    io.grpc.MethodDescriptor<io.notifyhub.v1.Empty, io.notifyhub.v1.PlatformList> getListPlatformsMethod;
    if ((getListPlatformsMethod = NotifyGrpc.getListPlatformsMethod) == null) {
      synchronized (NotifyGrpc.class) {
        if ((getListPlatformsMethod = NotifyGrpc.getListPlatformsMethod) == null) {
          NotifyGrpc.getListPlatformsMethod = getListPlatformsMethod =
              io.grpc.MethodDescriptor.<io.notifyhub.v1.Empty, io.notifyhub.v1.PlatformList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListPlatforms"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.PlatformList.getDefaultInstance()))
              .setSchemaDescriptor(new NotifyMethodDescriptorSupplier("ListPlatforms"))
              .build();
        }
      }
    }
    return getListPlatformsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.notifyhub.v1.PlatformRef,
      io.notifyhub.v1.Empty> getRemovePlatformMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RemovePlatform",
      requestType = io.notifyhub.v1.PlatformRef.class,
      responseType = io.notifyhub.v1.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.notifyhub.v1.PlatformRef,
      io.notifyhub.v1.Empty> getRemovePlatformMethod() {
    io.grpc.MethodDescriptor<io.notifyhub.v1.PlatformRef, io.notifyhub.v1.Empty> getRemovePlatformMethod;
    if ((getRemovePlatformMethod = NotifyGrpc.getRemovePlatformMethod) == null) {
      synchronized (NotifyGrpc.class) {
        if ((getRemovePlatformMethod = NotifyGrpc.getRemovePlatformMethod) == null) {
          NotifyGrpc.getRemovePlatformMethod = getRemovePlatformMethod =
              io.grpc.MethodDescriptor.<io.notifyhub.v1.PlatformRef, io.notifyhub.v1.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RemovePlatform"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.PlatformRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new NotifyMethodDescriptorSupplier("RemovePlatform"))
              .build();
        }
      }
    }
    return getRemovePlatformMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.notifyhub.v1.Empty,
      io.notifyhub.v1.Pong> getPingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Ping",
      requestType = io.notifyhub.v1.Empty.class,
      responseType = io.notifyhub.v1.Pong.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.notifyhub.v1.Empty,
      io.notifyhub.v1.Pong> getPingMethod() {
    io.grpc.MethodDescriptor<io.notifyhub.v1.Empty, io.notifyhub.v1.Pong> getPingMethod;
    if ((getPingMethod = NotifyGrpc.getPingMethod) == null) {
      synchronized (NotifyGrpc.class) {
        if ((getPingMethod = NotifyGrpc.getPingMethod) == null) {
          NotifyGrpc.getPingMethod = getPingMethod =
              io.grpc.MethodDescriptor.<io.notifyhub.v1.Empty, io.notifyhub.v1.Pong>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Ping"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.notifyhub.v1.Pong.getDefaultInstance()))
              .setSchemaDescriptor(new NotifyMethodDescriptorSupplier("Ping"))
              .build();
        }
      }
    }
    return getPingMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static NotifyStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NotifyStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NotifyStub>() {
        @java.lang.Override
        public NotifyStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NotifyStub(channel, callOptions);
        }
      };
    return NotifyStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static NotifyBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NotifyBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NotifyBlockingV2Stub>() {
        @java.lang.Override
        public NotifyBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NotifyBlockingV2Stub(channel, callOptions);
        }
      };
    return NotifyBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static NotifyBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NotifyBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NotifyBlockingStub>() {
        @java.lang.Override
        public NotifyBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NotifyBlockingStub(channel, callOptions);
        }
      };
    return NotifyBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static NotifyFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NotifyFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NotifyFutureStub>() {
        @java.lang.Override
        public NotifyFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NotifyFutureStub(channel, callOptions);
        }
      };
    return NotifyFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * NotifyHub —— 多语言通知推送服务。
   * 本文件是唯一契约：服务端将来重写（Go/Rust/Zig）时不得变更既有字段的编号与语义。
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * 发布通知：按路由推送到外部平台 + 广播给在线订阅者。
     * </pre>
     */
    default void publish(io.notifyhub.v1.PublishRequest request,
        io.grpc.stub.StreamObserver<io.notifyhub.v1.PublishAck> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPublishMethod(), responseObserver);
    }

    /**
     * <pre>
     * 订阅主题，服务端流式推送。
     * </pre>
     */
    default void subscribe(io.notifyhub.v1.SubscribeRequest request,
        io.grpc.stub.StreamObserver<io.notifyhub.v1.Event> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSubscribeMethod(), responseObserver);
    }

    /**
     * <pre>
     * 批量发布（双向流：每收到一条请求，回执一条 Ack）。
     * </pre>
     */
    default io.grpc.stub.StreamObserver<io.notifyhub.v1.PublishRequest> publishStream(
        io.grpc.stub.StreamObserver<io.notifyhub.v1.PublishAck> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getPublishStreamMethod(), responseObserver);
    }

    /**
     * <pre>
     * 用代码配置推送平台（运行时生效；重启后需重新注册，或写入配置文件）。
     * </pre>
     */
    default void upsertPlatform(io.notifyhub.v1.PlatformConfig request,
        io.grpc.stub.StreamObserver<io.notifyhub.v1.PlatformConfig> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpsertPlatformMethod(), responseObserver);
    }

    /**
     */
    default void listPlatforms(io.notifyhub.v1.Empty request,
        io.grpc.stub.StreamObserver<io.notifyhub.v1.PlatformList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListPlatformsMethod(), responseObserver);
    }

    /**
     */
    default void removePlatform(io.notifyhub.v1.PlatformRef request,
        io.grpc.stub.StreamObserver<io.notifyhub.v1.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRemovePlatformMethod(), responseObserver);
    }

    /**
     * <pre>
     * 健康检查（无需鉴权）。
     * </pre>
     */
    default void ping(io.notifyhub.v1.Empty request,
        io.grpc.stub.StreamObserver<io.notifyhub.v1.Pong> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPingMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service Notify.
   * <pre>
   * NotifyHub —— 多语言通知推送服务。
   * 本文件是唯一契约：服务端将来重写（Go/Rust/Zig）时不得变更既有字段的编号与语义。
   * </pre>
   */
  public static abstract class NotifyImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return NotifyGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service Notify.
   * <pre>
   * NotifyHub —— 多语言通知推送服务。
   * 本文件是唯一契约：服务端将来重写（Go/Rust/Zig）时不得变更既有字段的编号与语义。
   * </pre>
   */
  public static final class NotifyStub
      extends io.grpc.stub.AbstractAsyncStub<NotifyStub> {
    private NotifyStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NotifyStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NotifyStub(channel, callOptions);
    }

    /**
     * <pre>
     * 发布通知：按路由推送到外部平台 + 广播给在线订阅者。
     * </pre>
     */
    public void publish(io.notifyhub.v1.PublishRequest request,
        io.grpc.stub.StreamObserver<io.notifyhub.v1.PublishAck> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPublishMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 订阅主题，服务端流式推送。
     * </pre>
     */
    public void subscribe(io.notifyhub.v1.SubscribeRequest request,
        io.grpc.stub.StreamObserver<io.notifyhub.v1.Event> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getSubscribeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 批量发布（双向流：每收到一条请求，回执一条 Ack）。
     * </pre>
     */
    public io.grpc.stub.StreamObserver<io.notifyhub.v1.PublishRequest> publishStream(
        io.grpc.stub.StreamObserver<io.notifyhub.v1.PublishAck> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getPublishStreamMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * 用代码配置推送平台（运行时生效；重启后需重新注册，或写入配置文件）。
     * </pre>
     */
    public void upsertPlatform(io.notifyhub.v1.PlatformConfig request,
        io.grpc.stub.StreamObserver<io.notifyhub.v1.PlatformConfig> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpsertPlatformMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listPlatforms(io.notifyhub.v1.Empty request,
        io.grpc.stub.StreamObserver<io.notifyhub.v1.PlatformList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListPlatformsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void removePlatform(io.notifyhub.v1.PlatformRef request,
        io.grpc.stub.StreamObserver<io.notifyhub.v1.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRemovePlatformMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 健康检查（无需鉴权）。
     * </pre>
     */
    public void ping(io.notifyhub.v1.Empty request,
        io.grpc.stub.StreamObserver<io.notifyhub.v1.Pong> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPingMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service Notify.
   * <pre>
   * NotifyHub —— 多语言通知推送服务。
   * 本文件是唯一契约：服务端将来重写（Go/Rust/Zig）时不得变更既有字段的编号与语义。
   * </pre>
   */
  public static final class NotifyBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<NotifyBlockingV2Stub> {
    private NotifyBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NotifyBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NotifyBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * 发布通知：按路由推送到外部平台 + 广播给在线订阅者。
     * </pre>
     */
    public io.notifyhub.v1.PublishAck publish(io.notifyhub.v1.PublishRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getPublishMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 订阅主题，服务端流式推送。
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, io.notifyhub.v1.Event>
        subscribe(io.notifyhub.v1.SubscribeRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getSubscribeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 批量发布（双向流：每收到一条请求，回执一条 Ack）。
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<io.notifyhub.v1.PublishRequest, io.notifyhub.v1.PublishAck>
        publishStream() {
      return io.grpc.stub.ClientCalls.blockingBidiStreamingCall(
          getChannel(), getPublishStreamMethod(), getCallOptions());
    }

    /**
     * <pre>
     * 用代码配置推送平台（运行时生效；重启后需重新注册，或写入配置文件）。
     * </pre>
     */
    public io.notifyhub.v1.PlatformConfig upsertPlatform(io.notifyhub.v1.PlatformConfig request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUpsertPlatformMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.notifyhub.v1.PlatformList listPlatforms(io.notifyhub.v1.Empty request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListPlatformsMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.notifyhub.v1.Empty removePlatform(io.notifyhub.v1.PlatformRef request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRemovePlatformMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 健康检查（无需鉴权）。
     * </pre>
     */
    public io.notifyhub.v1.Pong ping(io.notifyhub.v1.Empty request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getPingMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service Notify.
   * <pre>
   * NotifyHub —— 多语言通知推送服务。
   * 本文件是唯一契约：服务端将来重写（Go/Rust/Zig）时不得变更既有字段的编号与语义。
   * </pre>
   */
  public static final class NotifyBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<NotifyBlockingStub> {
    private NotifyBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NotifyBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NotifyBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 发布通知：按路由推送到外部平台 + 广播给在线订阅者。
     * </pre>
     */
    public io.notifyhub.v1.PublishAck publish(io.notifyhub.v1.PublishRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPublishMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 订阅主题，服务端流式推送。
     * </pre>
     */
    public java.util.Iterator<io.notifyhub.v1.Event> subscribe(
        io.notifyhub.v1.SubscribeRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getSubscribeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 用代码配置推送平台（运行时生效；重启后需重新注册，或写入配置文件）。
     * </pre>
     */
    public io.notifyhub.v1.PlatformConfig upsertPlatform(io.notifyhub.v1.PlatformConfig request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpsertPlatformMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.notifyhub.v1.PlatformList listPlatforms(io.notifyhub.v1.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListPlatformsMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.notifyhub.v1.Empty removePlatform(io.notifyhub.v1.PlatformRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRemovePlatformMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 健康检查（无需鉴权）。
     * </pre>
     */
    public io.notifyhub.v1.Pong ping(io.notifyhub.v1.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPingMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service Notify.
   * <pre>
   * NotifyHub —— 多语言通知推送服务。
   * 本文件是唯一契约：服务端将来重写（Go/Rust/Zig）时不得变更既有字段的编号与语义。
   * </pre>
   */
  public static final class NotifyFutureStub
      extends io.grpc.stub.AbstractFutureStub<NotifyFutureStub> {
    private NotifyFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NotifyFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NotifyFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 发布通知：按路由推送到外部平台 + 广播给在线订阅者。
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.notifyhub.v1.PublishAck> publish(
        io.notifyhub.v1.PublishRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPublishMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 用代码配置推送平台（运行时生效；重启后需重新注册，或写入配置文件）。
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.notifyhub.v1.PlatformConfig> upsertPlatform(
        io.notifyhub.v1.PlatformConfig request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpsertPlatformMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.notifyhub.v1.PlatformList> listPlatforms(
        io.notifyhub.v1.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListPlatformsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.notifyhub.v1.Empty> removePlatform(
        io.notifyhub.v1.PlatformRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRemovePlatformMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 健康检查（无需鉴权）。
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.notifyhub.v1.Pong> ping(
        io.notifyhub.v1.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPingMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PUBLISH = 0;
  private static final int METHODID_SUBSCRIBE = 1;
  private static final int METHODID_UPSERT_PLATFORM = 2;
  private static final int METHODID_LIST_PLATFORMS = 3;
  private static final int METHODID_REMOVE_PLATFORM = 4;
  private static final int METHODID_PING = 5;
  private static final int METHODID_PUBLISH_STREAM = 6;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PUBLISH:
          serviceImpl.publish((io.notifyhub.v1.PublishRequest) request,
              (io.grpc.stub.StreamObserver<io.notifyhub.v1.PublishAck>) responseObserver);
          break;
        case METHODID_SUBSCRIBE:
          serviceImpl.subscribe((io.notifyhub.v1.SubscribeRequest) request,
              (io.grpc.stub.StreamObserver<io.notifyhub.v1.Event>) responseObserver);
          break;
        case METHODID_UPSERT_PLATFORM:
          serviceImpl.upsertPlatform((io.notifyhub.v1.PlatformConfig) request,
              (io.grpc.stub.StreamObserver<io.notifyhub.v1.PlatformConfig>) responseObserver);
          break;
        case METHODID_LIST_PLATFORMS:
          serviceImpl.listPlatforms((io.notifyhub.v1.Empty) request,
              (io.grpc.stub.StreamObserver<io.notifyhub.v1.PlatformList>) responseObserver);
          break;
        case METHODID_REMOVE_PLATFORM:
          serviceImpl.removePlatform((io.notifyhub.v1.PlatformRef) request,
              (io.grpc.stub.StreamObserver<io.notifyhub.v1.Empty>) responseObserver);
          break;
        case METHODID_PING:
          serviceImpl.ping((io.notifyhub.v1.Empty) request,
              (io.grpc.stub.StreamObserver<io.notifyhub.v1.Pong>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PUBLISH_STREAM:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.publishStream(
              (io.grpc.stub.StreamObserver<io.notifyhub.v1.PublishAck>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getPublishMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.notifyhub.v1.PublishRequest,
              io.notifyhub.v1.PublishAck>(
                service, METHODID_PUBLISH)))
        .addMethod(
          getSubscribeMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              io.notifyhub.v1.SubscribeRequest,
              io.notifyhub.v1.Event>(
                service, METHODID_SUBSCRIBE)))
        .addMethod(
          getPublishStreamMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              io.notifyhub.v1.PublishRequest,
              io.notifyhub.v1.PublishAck>(
                service, METHODID_PUBLISH_STREAM)))
        .addMethod(
          getUpsertPlatformMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.notifyhub.v1.PlatformConfig,
              io.notifyhub.v1.PlatformConfig>(
                service, METHODID_UPSERT_PLATFORM)))
        .addMethod(
          getListPlatformsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.notifyhub.v1.Empty,
              io.notifyhub.v1.PlatformList>(
                service, METHODID_LIST_PLATFORMS)))
        .addMethod(
          getRemovePlatformMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.notifyhub.v1.PlatformRef,
              io.notifyhub.v1.Empty>(
                service, METHODID_REMOVE_PLATFORM)))
        .addMethod(
          getPingMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.notifyhub.v1.Empty,
              io.notifyhub.v1.Pong>(
                service, METHODID_PING)))
        .build();
  }

  private static abstract class NotifyBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    NotifyBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.notifyhub.v1.NotifyProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Notify");
    }
  }

  private static final class NotifyFileDescriptorSupplier
      extends NotifyBaseDescriptorSupplier {
    NotifyFileDescriptorSupplier() {}
  }

  private static final class NotifyMethodDescriptorSupplier
      extends NotifyBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    NotifyMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (NotifyGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new NotifyFileDescriptorSupplier())
              .addMethod(getPublishMethod())
              .addMethod(getSubscribeMethod())
              .addMethod(getPublishStreamMethod())
              .addMethod(getUpsertPlatformMethod())
              .addMethod(getListPlatformsMethod())
              .addMethod(getRemovePlatformMethod())
              .addMethod(getPingMethod())
              .build();
        }
      }
    }
    return result;
  }
}
