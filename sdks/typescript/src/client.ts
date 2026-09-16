/**
 * NotifyHub TypeScript 客户端（gRPC 薄封装）。
 *
 * 通过 @grpc/proto-loader 运行时加载 proto，无需代码生成步骤。
 *
 * ```ts
 * import { NotifyClient } from "@notifyhub/client";
 * const client = new NotifyClient("localhost:9987", "ntf_xxx");
 * await client.publish("alert", "部署完成", "v1.2.0 上线");
 * ```
 */

import * as grpc from "@grpc/grpc-js";
import * as protoLoader from "@grpc/proto-loader";
import * as path from "path";

const PROTO_PATH = path.join(__dirname, "..", "proto", "notify", "v1", "notify.proto");

const loaderOptions: protoLoader.Options = {
  keepCase: false,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true,
};

/** 发布选项 */
export interface PublishOptions {
  /** 模板变量 */
  params?: Record<string, string>;
  /** 显式指定目标平台名（覆盖路由） */
  platforms?: string[];
  /** 去重键：窗口期内重复将被拒绝 */
  dedupKey?: string;
  /** 不广播给在线订阅者 */
  skipSubscribers?: boolean;
  /** 不推送到外部平台 */
  skipPlatforms?: boolean;
}

/** 批量发布的单条请求（字段名同 publish 的入参，camelCase）。 */
export interface PublishRequestInput {
  topic: string;
  title?: string;
  content?: string;
  params?: Record<string, string>;
  platforms?: string[];
  options?: {
    dedupKey?: string;
    skipSubscribers?: boolean;
    skipPlatforms?: boolean;
  };
}

export interface PublishAck {
  eventId: string;
  accepted: boolean;
  deduplicated: boolean;
  matchedPlatforms: string[];
  error?: string;
}

export interface Event {
  topic: string;
  title: string;
  content: string;
  params: Record<string, string>;
  eventId: string;
  /** unix 毫秒 */
  timestamp: string;
}

export interface PlatformConfig {
  name: string;
  /** dingtalk | wecom | feishu | webhook */
  type: string;
  webhook: string;
  secret?: string;
  topics?: string[];
  template?: string;
  atMobiles?: string[];
  extra?: Record<string, string>;
  rateLimitQps?: number;
}

export interface Subscription {
  /** 主动退订 */
  close(): void;
}

export class NotifyError extends Error {
  constructor(readonly code: grpc.status, details: string) {
    super(`${grpc.status[code]}: ${details}`);
  }
}

export class NotifyClient {
  private readonly client: any;
  private readonly metadata: grpc.Metadata;

  constructor(target: string, token?: string) {
    const definition = protoLoader.loadSync(PROTO_PATH, loaderOptions);
    const proto = grpc.loadPackageDefinition(definition) as any;
    this.client = new proto.notify.v1.Notify(target, grpc.credentials.createInsecure());
    this.metadata = new grpc.Metadata();
    if (token) {
      this.metadata.set("x-api-token", token);
    }
  }

  private promisify<T>(fn: string, req: object): Promise<T> {
    return new Promise((resolve, reject) => {
      this.client[fn](req, this.metadata, (err: grpc.ServiceError | null, resp: T) => {
        err ? reject(new NotifyError(err.code, err.details)) : resolve(resp);
      });
    });
  }

  /** 健康检查（免鉴权） */
  async ping(): Promise<{ version: string; uptimeSeconds: string }> {
    return this.promisify("Ping", {});
  }

  /**
   * 发布通知：按路由推送平台 + 广播给订阅者。
   */
  publish(topic: string, title: string, content: string = "", options: PublishOptions = {}): Promise<PublishAck> {
    const req: any = { topic, title, content };
    if (options.params) req.params = options.params;
    if (options.platforms) req.platforms = options.platforms;
    req.options = {
      dedupKey: options.dedupKey ?? "",
      skipSubscribers: options.skipSubscribers ?? false,
      skipPlatforms: options.skipPlatforms ?? false,
    };
    return this.promisify("Publish", req);
  }

  /**
   * 批量发布（双向流）：一次性发送并收集全部回执。
   *
   * 单条失败（topic 为空、平台不存在）不会中断流，对应回执的 accepted=false 且带 error。
   */
  publishBatch(requests: PublishRequestInput[], timeoutMs = 5000): Promise<PublishAck[]> {
    return new Promise((resolve, reject) => {
      const stream = this.client.PublishStream(this.metadata);
      const acks: PublishAck[] = [];
      const timer = setTimeout(() => {
        stream.cancel();
        reject(new NotifyError(grpc.status.DEADLINE_EXCEEDED, `批量发布超时 ${timeoutMs}ms`));
      }, timeoutMs);

      stream.on("data", (ack: any) => acks.push(ack as PublishAck));
      stream.on("error", (err: grpc.ServiceError) => {
        clearTimeout(timer);
        reject(new NotifyError(err.code, err.details));
      });
      stream.on("end", () => {
        clearTimeout(timer);
        resolve(acks);
      });

      for (const r of requests) {
        stream.write(r);
      }
      stream.end();
    });
  }

  /**
   * 订阅主题。回调在 gRPC 线程触发。
   */
  subscribe(topics: string[], onEvent: (event: Event) => void, onError?: (err: Error) => void): Subscription {
    const stream = this.client.Subscribe({ topics }, this.metadata);
    stream.on("data", (msg: any) => onEvent(msg as Event));
    stream.on("error", (err: grpc.ServiceError) => {
      if (err.code === grpc.status.CANCELLED) return; // 主动退订
      onError ? onError(new NotifyError(err.code, err.details)) : undefined;
    });
    return { close: () => stream.cancel() };
  }

  // ---------- Admin ----------

  /** 用代码注册/更新推送平台（运行时生效，重启后需重新注册或写入配置文件）。 */
  async upsertPlatform(cfg: PlatformConfig): Promise<PlatformConfig> {
    return this.promisify("UpsertPlatform", cfg);
  }

  async listPlatforms(): Promise<{ platforms: PlatformConfig[] }> {
    return this.promisify("ListPlatforms", {});
  }

  async removePlatform(name: string): Promise<void> {
    return this.promisify("RemovePlatform", { name });
  }

  close(): void {
    this.client.close();
  }
}
