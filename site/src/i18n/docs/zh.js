const REPO = 'https://github.com/huangwenfu750/notifyhub'

export default {
  nav: { docs: '文档', overview: '文档首页', backHome: '返回首页', onThisPage: '本页目录' },

  index: {
    title: '文档',
    lead: '从启动服务端到多语言接入、配置逐项说明、gRPC 契约与投递语义。三份文档覆盖完整使用链路。',
    cards: [
      { slug: 'usage', title: '使用说明', desc: '启动服务端、最小配置、核心概念、各语言 SDK、运行时管理与排错速查。' },
      { slug: 'config', title: '配置手册', desc: 'server / auth / defaults / platforms[] 逐项说明，含默认值与覆盖规则。' },
      { slug: 'protocol', title: '协议参考', desc: 'notify.v1 的六个方法、鉴权、投递语义与四个渠道的加签实现细节。' },
    ],
    externalTitle: '仓库内原文',
    external: [
      { text: 'README（总览）', href: `${REPO}#readme` },
      { text: 'docs/usage.md', href: `${REPO}/tree/main/docs/usage.md` },
      { text: 'docs/config.md', href: `${REPO}/tree/main/docs/config.md` },
      { text: 'docs/protocol.md', href: `${REPO}/tree/main/docs/protocol.md` },
      { text: 'config.example.yaml', href: `${REPO}/blob/main/config.example.yaml` },
    ],
  },

  usage: {
    title: '使用说明',
    desc: '面向第一次上手：从启动服务端到多语言发通知、收通知、动态配置平台，以及排错速查。',
    sections: [
      {
        id: 'mindmodel',
        title: '0. 心智模型',
        blocks: [
          { type: 'p', text: 'NotifyHub 是一个单进程 gRPC 服务，干两件事：' },
          {
            type: 'list',
            ordered: true,
            items: [
              '把通知转发出去：Publish(topic, title, content) → 服务端按 topic 通配路由到已配置的推送平台 → 异步投递（限流 + 重试 + 去重 + 死信）。',
              '订阅主题：Subscribe(["alert.*"]) → 任何语言发布的事件实时推给你（至多一次，不持久化）。',
            ],
          },
          {
            type: 'code',
            lang: 'text',
            code: '你的服务 ──gRPC(x-api-token)──▶ NotifyHub ──HTTP──▶ 钉钉 / 企微 / 飞书 / Webhook\n                                   │\n                                   └──stream──▶ 其他语言的订阅者',
          },
          { type: 'p', text: '客户端不需要 HTTP、不需要签名、不需要知道机器人地址 —— 这些都在服务端配置里。' },
        ],
      },
      {
        id: 'start',
        title: '1. 启动服务端',
        blocks: [
          { type: 'h3', text: '1.1 构建（Java 21 + Gradle）' },
          { type: 'p', text: '仓库没有自带 gradle wrapper，使用系统安装的 gradle：' },
          {
            type: 'code',
            lang: 'bash',
            code: './scripts/gen-protos.sh java     # 首次：生成 Java gRPC stub（自动下载 protoc）\ngradle :server:installDist       # 产出发行版到 server/build/install/server/',
          },
          { type: 'p', text: 'Windows 下：' },
          { type: 'code', lang: 'bash', code: 'bash scripts\\gen-protos.sh java\ngradle :server:installDist' },
          { type: 'h3', text: '1.2 准备配置' },
          { type: 'code', lang: 'bash', code: 'cp config.example.yaml config.yaml   # 然后编辑：填真实 webhook/secret 与 auth.tokens' },
          { type: 'h3', text: '1.3 运行' },
          {
            type: 'code',
            lang: 'bash',
            code: '# 方式 A：显式指定\n./server/build/install/server/bin/server --config config.yaml\n\n# 方式 B：环境变量（Docker 镜像内即此方式）\nexport NOTIFYHUB_CONFIG=/etc/notifyhub/config.yaml\n./server/build/install/server/bin/server\n\n# 方式 C：不带任何参数 → 读工作目录下的 config.yaml\ncd server/build/install/server && ./bin/server',
          },
          { type: 'p', text: '配置路径优先级：--config / -c 参数 > 环境变量 NOTIFYHUB_CONFIG > 工作目录 config.yaml。查看版本：server --version（不启动服务）。' },
          { type: 'p', text: '启动成功日志：' },
          {
            type: 'code',
            lang: 'text',
            code: '[main] INFO io.notifyhub.Server - NotifyHub 0.1.1 已启动，监听 0.0.0.0:9987 (tokens=1, platforms=3, workers=16)',
          },
          { type: 'note', text: '端口占用、平台类型写错、YAML 语法错误都会在启动时立即报错退出，不会带病运行。' },
          { type: 'h3', text: '1.4 Docker' },
          {
            type: 'code',
            lang: 'bash',
            code: 'docker compose up --build -d      # 挂载 ./config.yaml 到 /etc/notifyhub/config.yaml\ndocker logs -f notifyhub\ndocker compose down',
          },
          { type: 'p', text: '把 docker-compose.yml 的挂载源改成你自己的 config.yaml 即可。镜像内置了 TCP 9987 健康检查。' },
        ],
      },
      {
        id: 'minimal',
        title: '2. 最小可跑配置',
        blocks: [
          { type: 'p', text: '不想先申请机器人？用 webhook 类型打到本地回显服务，先跑通链路：' },
          {
            type: 'code',
            lang: 'yaml',
            code: 'server:\n  host: 127.0.0.1\n  port: 9987\n\nauth:\n  tokens: []            # 空数组 = 不鉴权，仅本机调试用\n\nplatforms:\n  - name: local-hook\n    type: webhook\n    url: http://127.0.0.1:19800/hook\n    topics: ["*"]',
          },
          { type: 'p', text: '再起一个回显服务：' },
          {
            type: 'code',
            lang: 'bash',
            code: 'python -c "from http.server import BaseHTTPRequestHandler,HTTPServer; \\\nHTTPServer((\'127.0.0.1\',19800), type(\'H\',(BaseHTTPRequestHandler,),{\'do_POST\':lambda s:(print(s.rfile.read(int(s.headers[\'Content-Length\']))), s.send_response(200), s.end_headers())})).serve_forever()"',
          },
          { type: 'note', text: '生产配置请把 auth.tokens 填上。' },
        ],
      },
      {
        id: 'concepts',
        title: '3. 核心概念',
        blocks: [
          { type: 'h3', text: '3.1 topic 与通配路由' },
          { type: 'p', text: '以 . 分段，AMQP 风格：' },
          {
            type: 'table',
            head: ['规则', '含义', '示例'],
            rows: [
              ['alert', '精确匹配', '只匹配 alert'],
              ['alert.*', '* 恰好一段', '匹配 alert.db，不匹配 alert、alert.db.slow'],
              ['ops.#', '# 零或多段，只能作末段', '匹配 ops、ops.a、ops.a.b'],
              ['*', '一段', '兜底常用 "*"'],
            ],
          },
          { type: 'p', text: '一条发布可以命中多个平台（按配置文件声明顺序全部入队）。' },
          { type: 'h3', text: '3.2 模板占位符' },
          {
            type: 'code',
            lang: 'yaml',
            code: 'template: "**{{title}}**\\n{{content}}\\n> topic={{topic}} host={{params.host}}"',
          },
          { type: 'p', text: '支持 {{title}}、{{content}}、{{topic}}、{{event_id}}、{{params.xxx}}，也可以直接写 {{xxx}}（等价于 params.xxx）。未知占位符替换为空字符串。' },
          { type: 'h3', text: '3.3 去重（防告警风暴）' },
          { type: 'code', lang: 'python', code: 'client.publish("alert.db", "磁盘告警", "...", dedup_key="db-01-disk")' },
          { type: 'p', text: '窗口期（默认 60s，由 defaults.dedup_window_ms 控制）内相同 key 的后续发布：返回 accepted=false, deduplicated=true，既不推平台也不广播订阅者。典型用法：dedup_key = 告警名 + 实例，让同一实例的同种告警 60s 内只响一次。' },
          { type: 'h3', text: '3.4 投递语义' },
          {
            type: 'list',
            ordered: true,
            items: [
              'Publish 只做「路由 + 去重 + 入队」，同步返回；真正的 HTTP 投递是异步的。',
              '每平台独立令牌桶限流（默认 15 qps，钉钉建议 ≤20）。',
              '失败按 backoff_ms × 2^n + 抖动指数退避重试，单次退避上限 30s。',
              '重试耗尽 → 写 ERROR 死信日志，并广播到内置主题 deadletter。',
            ],
          },
          {
            type: 'code',
            lang: 'python',
            code: 'client.subscribe(["deadletter"], lambda e: print("投递失败:", e.params))\n# params 含 platform / topic / event_id / error',
          },
          { type: 'h3', text: '3.5 订阅语义（重要）' },
          {
            type: 'list',
            items: [
              '至多一次：不持久化、不重放、不补发。',
              '订阅者处理太慢 → 事件被丢弃（服务端每订阅一个队列，慢消费者不阻塞别人）。',
              '客户端断开即自动退订；也可用句柄主动 cancel() / close()。',
              '需要「不丢消息」的场景：请让订阅端把事件落到自己的存储里。',
            ],
          },
        ],
      },
      {
        id: 'sdk',
        title: '4. 各语言 SDK',
        blocks: [
          { type: 'p', text: '官方 SDK 覆盖 Java、Python、TypeScript/JS、Go，外加 Spring Boot Starter。所有 SDK 都是薄封装：token 通过 gRPC metadata x-api-token 注入，底层就是 proto 里的那 6 个方法。' },
          { type: 'note', text: '首屏的 SDK 区块有五种语言的可运行示例；各语言完整用法（含订阅、Admin、错误处理）见仓库内 docs/usage.md 第 5 节。' },
          {
            type: 'table',
            head: ['语言', '安装', '来源'],
            rows: [
              ['Go', 'go get github.com/huangwenfu750/notifyhub/sdks/go@v0.1.1', 'sdks/go/v* 标签'],
              ['Java / Kotlin', 'implementation("io.github.huangwenfu750:sdk-java:0.1.1")', 'Maven Central'],
              ['Spring Boot', 'implementation("io.github.huangwenfu750:notifyhub-spring-boot-starter:0.1.1")', 'Maven Central'],
              ['TypeScript / JS', 'npm i notifyhub-client', 'npm'],
              ['Python', 'pip install notifyhub-client', 'PyPI'],
            ],
          },
        ],
      },
      {
        id: 'admin',
        title: '5. 运行时管理（Admin）',
        blocks: [
          { type: 'p', text: '平台配置有两条来源：' },
          {
            type: 'table',
            head: ['来源', '生效时机', '是否持久化'],
            rows: [
              ['YAML platforms[]', '启动时加载', '✅ 配置文件里'],
              ['UpsertPlatform RPC', '立即生效', '❌ 仅内存，重启丢失'],
            ],
          },
          { type: 'note', text: '实践建议：稳态平台写 YAML，临时 / 多租户场景用 Admin 动态注册（重启后需重新注册）。' },
          { type: 'p', text: 'Admin 会校验 name/type/webhook 非空、type 必须是已注册渠道、webhook 必须 http(s):// 开头，否则 INVALID_ARGUMENT；同名覆盖；RemovePlatform 不存在的名字返回 NOT_FOUND。' },
        ],
      },
      {
        id: 'troubleshoot',
        title: '6. 排错速查',
        blocks: [
          {
            type: 'table',
            head: ['现象 / 状态码', '原因', '处理'],
            rows: [
              ['UNAUTHENTICATED', '缺 x-api-token 或不在白名单', '检查 auth.tokens 与客户端 token 是否一致'],
              ['INVALID_ARGUMENT: topic 不能为空', '发布未带 topic', '补 topic'],
              ['NOT_FOUND: 平台不存在', 'platforms 指定了未注册的名字', 'list_platforms() 先确认'],
              ['UNAVAILABLE: 投递队列已满', '瞬时洪峰，queue_capacity 打满', '客户端退避重试；调大 queue_capacity / workers'],
              ['accepted=false, deduplicated=true', '命中去重窗口', '正常行为，换 dedup_key 或等窗口过去'],
              ['没收到消息但 accepted=true', '① 订阅还没建立就发布 ② 通配不匹配 ③ 平台侧真失败', '订阅后 sleep 片刻再发；核对通配；订阅 deadletter'],
              ['启动时报「未知平台类型」', 'type 拼错', '只能是 dingtalk / wecom / feishu / webhook'],
              ['收不到钉钉消息', '加签 secret 错 / 机器人安全设置 / 限流 20qps', '看服务端 ERROR 日志，rate_limit_qps 调到 ≤20'],
            ],
          },
          { type: 'p', text: '服务端日志（slf4j-simple，直接打到 stdout）是主要排查手段：投递成功 INFO、失败 WARN（带 attempt）、死信 ERROR。' },
        ],
      },
      {
        id: 'limits',
        title: '7. 已知边界（0.1.1）',
        blocks: [
          {
            type: 'list',
            items: [
              '订阅为至多一次，不持久化、不重放；订阅端慢消费会丢事件。',
              '平台投递异步、尽力而为：Publish 返回 accepted=true 只代表已入队，不代表平台已收到。',
              'Admin 注册的平台重启即失效。',
              '尚无 Web 控制台、无投递记录查询。',
              '浏览器直连需 gRPC-Web 网关。',
            ],
          },
        ],
      },
      {
        id: 'checklist',
        title: '8. 三分钟上手清单',
        blocks: [
          {
            type: 'list',
            ordered: true,
            items: [
              './scripts/gen-protos.sh java && gradle :server:installDist',
              '写一份最小 config.yaml（无鉴权 + 本地 webhook）',
              '起回显服务（19800）+ 起 NotifyHub（9987）',
              'pip install -e sdks/python，跑 examples/python/publish.py',
              '看到回显服务打印出 JSON，链路就通了 → 换成真实机器人 + 打开 auth.tokens',
            ],
          },
        ],
      },
    ],
  },

  config: {
    title: '配置手册',
    desc: '服务端读取 YAML 配置，优先级：--config 参数 > 环境变量 NOTIFYHUB_CONFIG > 工作目录 config.yaml。',
    sections: [
      {
        id: 'server',
        title: 'server',
        blocks: [
          {
            type: 'table',
            head: ['键', '默认', '说明'],
            rows: [
              ['host', '0.0.0.0', 'gRPC 监听地址'],
              ['port', '9987', 'gRPC 监听端口'],
              ['workers', 'CPU 核数（最小 4）', '平台投递 worker 线程数'],
              ['queue_capacity', '10000', '异步投递队列容量；打满时 Publish 返回 UNAVAILABLE'],
            ],
          },
        ],
      },
      {
        id: 'auth',
        title: 'auth',
        blocks: [
          { type: 'p', text: 'tokens：API token 白名单。' },
          { type: 'note', text: '为空数组时服务不鉴权（仅建议内网 / 本机调试使用）。' },
        ],
      },
      {
        id: 'defaults',
        title: 'defaults（全局默认，可被平台覆盖）',
        blocks: [
          {
            type: 'table',
            head: ['键', '默认', '说明'],
            rows: [
              ['retry.max_attempts', '3', '单条投递最大尝试次数（含首次）'],
              ['retry.backoff_ms', '500', '退避基数，指数增长 + 随机抖动，单次上限 30s'],
              ['rate_limit.qps', '15', '每平台令牌桶限流（钉钉官方上限 20 qps）'],
              ['dedup_window_ms', '60000', 'dedup_key 去重窗口'],
            ],
          },
        ],
      },
      {
        id: 'platforms',
        title: 'platforms[]（推送平台）',
        blocks: [
          {
            type: 'table',
            head: ['键', '说明'],
            rows: [
              ['name', '必填，全局唯一（重复启动报错）'],
              ['type', '必填：dingtalk / wecom / feishu / webhook'],
              ['webhook / url', '必填（两键等价），目标地址，http(s)://'],
              ['secret', '加签密钥：钉钉 / 飞书机器人的 SEC...；webhook 渠道为 HMAC 签名密钥'],
              ['topics', '路由规则数组，通配符 *（一段）/ #（多段）；缺省 ["*"] 全匹配'],
              ['template', '可选。占位符模板；缺省 title\\ncontent'],
              ['at_mobiles', '可选。钉钉 @ 手机号'],
              ['sign_header', '可选（webhook）。签名头名称，默认 X-Signature'],
              ['retry', '可选 {max_attempts, backoff_ms} 覆盖默认'],
              ['rate_limit_qps', '可选。覆盖默认 qps'],
            ],
          },
        ],
      },
      {
        id: 'runtime',
        title: '运行时管理',
        blocks: [
          { type: 'p', text: 'Admin RPC（UpsertPlatform / ListPlatforms / RemovePlatform）可在不重启的情况下增删改平台；运行时注册的平台仅存于内存，重启后以配置文件为准。' },
        ],
      },
    ],
  },

  protocol: {
    title: '协议参考',
    desc: '协议契约是 proto/notify/v1/notify.proto —— 该文件是唯一契约，字段编号与语义冻结。',
    sections: [
      {
        id: 'auth',
        title: '鉴权',
        blocks: [
          {
            type: 'list',
            items: [
              '服务端配置 auth.tokens 后，除 Ping 外所有方法要求 metadata 携带 x-api-token: <token>，否则返回 UNAUTHENTICATED。',
              'auth.tokens 为空时不鉴权。',
            ],
          },
        ],
      },
      {
        id: 'publish',
        title: 'Notify.Publish（一元）',
        blocks: [
          { type: 'p', text: '发布一条通知：按路由推送到平台 + 广播给在线订阅者。' },
          {
            type: 'table',
            head: ['字段', '说明'],
            rows: [
              ['topic', '必填。业务主题，如 alert.db；为空返回 INVALID_ARGUMENT'],
              ['title / content', '通知标题与正文'],
              ['params', '模板变量 map<string,string>'],
              ['platforms', '显式指定目标平台名，覆盖路由；不存在返回 NOT_FOUND'],
              ['options.dedup_key', '非空时启用去重：窗口期内重复 key 返回 accepted=false, deduplicated=true'],
              ['options.skip_subscribers', '不广播给订阅者'],
              ['options.skip_platforms', '不推送平台；此时 ack 的 matched_platforms 为空'],
            ],
          },
          { type: 'p', text: '成功返回 PublishAck{event_id, accepted=true, matched_platforms}（实际入队投递的平台）。投递队列打满返回 UNAVAILABLE。' },
        ],
      },
      {
        id: 'subscribe',
        title: 'Notify.Subscribe（服务端流）',
        blocks: [
          {
            type: 'list',
            items: [
              'topics 支持 AMQP 风格通配符：* 恰好一段、# 零或多段（. 分隔）；# 只能作为末段。',
              '语义：至多一次。慢消费者事件丢弃、不重放、不持久化。',
              '客户端断开即自动退订。',
            ],
          },
        ],
      },
      {
        id: 'publish-stream',
        title: 'Notify.PublishStream（双向流）',
        blocks: [
          { type: 'p', text: '批量发布：每收到一条 PublishRequest 回执一条 PublishAck。与 Publish 的差异：单条错误（如 topic 为空、平台不存在）不中断流，而是返回 accepted=false, error="..." 的回执。' },
        ],
      },
      {
        id: 'admin',
        title: 'UpsertPlatform / ListPlatforms / RemovePlatform',
        blocks: [
          {
            type: 'list',
            items: [
              'UpsertPlatform：校验 name/type/webhook，type 必须是已注册渠道（dingtalk | wecom | feishu | webhook），非法返回 INVALID_ARGUMENT。同名覆盖。',
              '平台配置运行时生效，不持久化 —— 重启后需重新注册或写入配置文件。',
              'RemovePlatform 对不存在的名字返回 NOT_FOUND。',
            ],
          },
        ],
      },
      {
        id: 'ping',
        title: 'Notify.Ping',
        blocks: [{ type: 'p', text: '健康检查，免鉴权。返回版本与运行时长。' }],
      },
      {
        id: 'delivery',
        title: '投递语义（服务端 → 平台）',
        blocks: [
          {
            type: 'list',
            ordered: true,
            items: [
              'Publish 同步完成路由 / 去重 / 入队后即返回 ack；平台投递异步进行。',
              '每平台独立令牌桶限流（默认 15 qps，可配）。',
              '失败指数退避重试（默认 3 次，backoff_ms 基数 + 随机抖动，单次上限 30s）。',
              '最终失败写入死信日志，并广播到内置主题 deadletter，事件 params 含 platform/topic/event_id/error。',
            ],
          },
        ],
      },
      {
        id: 'channels',
        title: '各渠道实现细节',
        blocks: [
          {
            type: 'table',
            head: ['渠道', '加签', '请求', '成功判定'],
            rows: [
              ['dingtalk', 'urlencode(base64(HmacSHA256(key=secret, data=timestamp+"\\n"+secret))) 拼入 URL', 'msgtype=markdown，支持 at_mobiles', 'errcode==0'],
              ['wecom', '无（key 在 URL）', 'msgtype=markdown，正文上限约 2000 字符', 'errcode==0'],
              ['feishu', 'base64(HmacSHA256(key=timestamp+"\\n"+secret, data="")) 放入 body', 'msg_type=text', 'code==0 或 StatusCode==0'],
              ['webhook', '可选 extra.sign_header（默认 X-Signature），值为 hex(HmacSHA256(key=secret, data=原始请求体))', 'JSON：{event_id, topic, title, content, params, timestamp}', 'HTTP 2xx'],
            ],
          },
          { type: 'p', text: '模板占位符：{{title}}、{{content}}、{{topic}}、{{event_id}}、{{params.xxx}}（或直接 {{xxx}}）；未知占位符替换为空。' },
        ],
      },
    ],
  },
}
