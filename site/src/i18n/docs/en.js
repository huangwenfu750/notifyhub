const REPO = 'https://github.com/huangwenfu750/notifyhub'

export default {
  nav: { docs: 'Docs', overview: 'Docs home', backHome: 'Back to home', onThisPage: 'On this page' },

  index: {
    title: 'Documentation',
    lead: 'From starting the server to multi-language clients, every config key, and the gRPC contract with delivery semantics. Three docs cover the whole path.',
    cards: [
      { slug: 'usage', title: 'Usage guide', desc: 'Start the server, a minimal config, core concepts, every SDK, runtime management and a troubleshooting table.' },
      { slug: 'config', title: 'Configuration', desc: 'server / auth / defaults / platforms[] key by key, with defaults and override rules.' },
      { slug: 'protocol', title: 'Protocol reference', desc: 'The six notify.v1 methods, authentication, delivery semantics and per-channel signing details.' },
    ],
    externalTitle: 'Source in the repo',
    external: [
      { text: 'README (overview)', href: `${REPO}#readme` },
      { text: 'docs/usage.en.md', href: `${REPO}/tree/main/docs/usage.en.md` },
      { text: 'docs/config.en.md', href: `${REPO}/tree/main/docs/config.en.md` },
      { text: 'docs/protocol.en.md', href: `${REPO}/tree/main/docs/protocol.en.md` },
      { text: 'config.example.yaml', href: `${REPO}/blob/main/config.example.yaml` },
    ],
  },

  usage: {
    title: 'Usage guide',
    desc: 'For first-time users: start the server, publish and subscribe from any language, configure platforms at runtime, plus a troubleshooting table.',
    sections: [
      {
        id: 'mindmodel',
        title: '0. Mental model',
        blocks: [
          { type: 'p', text: 'NotifyHub is a single-process gRPC service that does two things:' },
          {
            type: 'list',
            ordered: true,
            items: [
              'Forward notifications: Publish(topic, title, content) → the server routes by topic wildcard to the configured platforms → delivers asynchronously (rate limit + retry + dedup + dead letter).',
              'Subscribe to topics: Subscribe(["alert.*"]) → events published from any language arrive live (at-most-once, never persisted).',
            ],
          },
          {
            type: 'code',
            lang: 'text',
            code: 'your service ──gRPC(x-api-token)──▶ NotifyHub ──HTTP──▶ DingTalk / WeCom / Feishu / webhook\n                                       │\n                                       └──stream──▶ subscribers in other languages',
          },
          { type: 'p', text: 'Clients need no HTTP, no signing, and no knowledge of bot addresses — all of that lives in the server config.' },
        ],
      },
      {
        id: 'start',
        title: '1. Start the server',
        blocks: [
          { type: 'h3', text: '1.1 Build (Java 21 + Gradle)' },
          { type: 'p', text: 'The repo ships no gradle wrapper — use the gradle installed on your system:' },
          {
            type: 'code',
            lang: 'bash',
            code: './scripts/gen-protos.sh java     # first run: generate Java gRPC stubs (downloads protoc)\ngradle :server:installDist       # produces the distribution in server/build/install/server/',
          },
          { type: 'p', text: 'On Windows:' },
          { type: 'code', lang: 'bash', code: 'bash scripts\\gen-protos.sh java\ngradle :server:installDist' },
          { type: 'h3', text: '1.2 Prepare the config' },
          { type: 'code', lang: 'bash', code: 'cp config.example.yaml config.yaml   # then edit: real webhook/secret and auth.tokens' },
          { type: 'h3', text: '1.3 Run' },
          {
            type: 'code',
            lang: 'bash',
            code: '# A: explicit path\n./server/build/install/server/bin/server --config config.yaml\n\n# B: environment variable (this is what the Docker image uses)\nexport NOTIFYHUB_CONFIG=/etc/notifyhub/config.yaml\n./server/build/install/server/bin/server\n\n# C: no arguments → reads config.yaml from the working directory\ncd server/build/install/server && ./bin/server',
          },
          { type: 'p', text: 'Config precedence: --config / -c argument > NOTIFYHUB_CONFIG env var > config.yaml in the working directory. Check the version with server --version (does not start the service).' },
          { type: 'p', text: 'Successful startup log:' },
          {
            type: 'code',
            lang: 'text',
            code: '[main] INFO io.notifyhub.Server - NotifyHub 0.1.1 started, listening on 0.0.0.0:9987 (tokens=1, platforms=3, workers=16)',
          },
          { type: 'note', text: 'Port conflicts, wrong platform types and YAML syntax errors all fail fast at startup — the server never runs half-configured.' },
          { type: 'h3', text: '1.4 Docker' },
          {
            type: 'code',
            lang: 'bash',
            code: 'docker compose up --build -d      # mounts ./config.yaml to /etc/notifyhub/config.yaml\ndocker logs -f notifyhub\ndocker compose down',
          },
          { type: 'p', text: 'Point the docker-compose.yml mount at your own config.yaml. The image ships a TCP 9987 healthcheck.' },
        ],
      },
      {
        id: 'minimal',
        title: '2. Minimal runnable config',
        blocks: [
          { type: 'p', text: 'Not ready to create a bot yet? Use the webhook type against a local echo server to prove the path first:' },
          {
            type: 'code',
            lang: 'yaml',
            code: 'server:\n  host: 127.0.0.1\n  port: 9987\n\nauth:\n  tokens: []            # empty array = no auth, local debugging only\n\nplatforms:\n  - name: local-hook\n    type: webhook\n    url: http://127.0.0.1:19800/hook\n    topics: ["*"]',
          },
          { type: 'p', text: 'Then start an echo server:' },
          {
            type: 'code',
            lang: 'bash',
            code: 'python -c "from http.server import BaseHTTPRequestHandler,HTTPServer; \\\nHTTPServer((\'127.0.0.1\',19800), type(\'H\',(BaseHTTPRequestHandler,),{\'do_POST\':lambda s:(print(s.rfile.read(int(s.headers[\'Content-Length\']))), s.send_response(200), s.end_headers())})).serve_forever()"',
          },
          { type: 'note', text: 'Fill in auth.tokens before going to production.' },
        ],
      },
      {
        id: 'concepts',
        title: '3. Core concepts',
        blocks: [
          { type: 'h3', text: '3.1 Topics and wildcard routing' },
          { type: 'p', text: 'Dot-separated segments, AMQP style:' },
          {
            type: 'table',
            head: ['Pattern', 'Meaning', 'Example'],
            rows: [
              ['alert', 'exact match', 'matches only alert'],
              ['alert.*', '* is exactly one segment', 'matches alert.db, not alert or alert.db.slow'],
              ['ops.#', '# is zero or more segments, last only', 'matches ops, ops.a, ops.a.b'],
              ['*', 'one segment', 'commonly used as a catch-all'],
            ],
          },
          { type: 'p', text: 'A single publish can hit several platforms (all matching ones are queued, in declaration order).' },
          { type: 'h3', text: '3.2 Template placeholders' },
          {
            type: 'code',
            lang: 'yaml',
            code: 'template: "**{{title}}**\\n{{content}}\\n> topic={{topic}} host={{params.host}}"',
          },
          { type: 'p', text: 'Supported: {{title}}, {{content}}, {{topic}}, {{event_id}}, {{params.xxx}} — or just {{xxx}}, which equals params.xxx. Unknown placeholders become empty strings.' },
          { type: 'h3', text: '3.3 Deduplication (alert-storm guard)' },
          { type: 'code', lang: 'python', code: 'client.publish("alert.db", "Disk alert", "...", dedup_key="db-01-disk")' },
          { type: 'p', text: 'Within the window (default 60s, controlled by defaults.dedup_window_ms), later publishes with the same key return accepted=false, deduplicated=true — neither delivered nor broadcast. Common usage: dedup_key = alert name + instance, so one instance fires the same alert only once per 60s.' },
          { type: 'h3', text: '3.4 Delivery semantics' },
          {
            type: 'list',
            ordered: true,
            items: [
              'Publish only routes, dedups and enqueues — it returns synchronously; the actual HTTP delivery is asynchronous.',
              'Each platform gets its own token bucket (default 15 qps; DingTalk recommends ≤20).',
              'Failures retry with backoff_ms × 2^n + jitter, single backoff capped at 30s.',
              'When retries are exhausted: an ERROR dead-letter log plus a broadcast on the built-in topic deadletter.',
            ],
          },
          {
            type: 'code',
            lang: 'python',
            code: 'client.subscribe(["deadletter"], lambda e: print("delivery failed:", e.params))\n# params contains platform / topic / event_id / error',
          },
          { type: 'h3', text: '3.5 Subscription semantics (important)' },
          {
            type: 'list',
            items: [
              'At-most-once: no persistence, no replay, no redelivery.',
              'Slow consumers drop events (each subscription has its own queue, so one slow client never blocks others).',
              'Disconnecting unsubscribes automatically; you can also cancel() / close() the handle.',
              'If you cannot afford to lose events, persist them on the subscriber side.',
            ],
          },
        ],
      },
      {
        id: 'sdk',
        title: '4. SDKs per language',
        blocks: [
          { type: 'p', text: 'Official SDKs cover Java, Python, TypeScript/JS and Go, plus a Spring Boot starter. Every SDK is a thin wrapper: the token goes into the x-api-token gRPC metadata, and underneath are just the six methods from the proto.' },
          { type: 'note', text: 'The homepage SDK section has runnable snippets for all five options; the full per-language walkthrough (subscribe, Admin, error handling) lives in docs/usage.en.md section 5 in the repo.' },
          {
            type: 'table',
            head: ['Language', 'Install', 'Source'],
            rows: [
              ['Go', 'go get github.com/huangwenfu750/notifyhub/sdks/go@v0.1.1', 'sdks/go/v* tags'],
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
        title: '5. Runtime management (Admin)',
        blocks: [
          { type: 'p', text: 'Platform config comes from two sources:' },
          {
            type: 'table',
            head: ['Source', 'Takes effect', 'Persisted'],
            rows: [
              ['YAML platforms[]', 'loaded at startup', '✅ in the config file'],
              ['UpsertPlatform RPC', 'immediately', '❌ in memory only, lost on restart'],
            ],
          },
          { type: 'note', text: 'Rule of thumb: keep stable platforms in YAML, use Admin for temporary or multi-tenant platforms (re-register after a restart).' },
          { type: 'p', text: 'Admin validates that name/type/webhook are non-empty, that type is a registered channel, and that webhook starts with http(s):// — otherwise INVALID_ARGUMENT. Same name overwrites; RemovePlatform returns NOT_FOUND for unknown names.' },
        ],
      },
      {
        id: 'troubleshoot',
        title: '6. Troubleshooting',
        blocks: [
          {
            type: 'table',
            head: ['Symptom / status', 'Cause', 'Fix'],
            rows: [
              ['UNAUTHENTICATED', 'missing x-api-token or not whitelisted', 'check auth.tokens against the client token'],
              ['INVALID_ARGUMENT: topic must not be empty', 'publish without a topic', 'set the topic'],
              ['NOT_FOUND: platform does not exist', 'platforms named an unregistered platform', 'call list_platforms() first'],
              ['UNAVAILABLE: delivery queue full', 'burst traffic filled queue_capacity', 'back off and retry; raise queue_capacity / workers'],
              ['accepted=false, deduplicated=true', 'hit the dedup window', 'expected — change dedup_key or wait it out'],
              ['accepted=true but nothing received', '① published before subscribing ② wildcard mismatch ③ the platform really failed', 'sleep briefly after subscribing; check the wildcard; subscribe to deadletter'],
              ['unknown platform type at startup', 'typo in type', 'only dingtalk / wecom / feishu / webhook'],
              ['no DingTalk messages', 'wrong signing secret / bot security settings / 20 qps limit', 'check server ERROR logs, set rate_limit_qps ≤ 20'],
            ],
          },
          { type: 'p', text: 'Server logs (slf4j-simple, straight to stdout) are the main tool: INFO on success, WARN on failure (with attempt), ERROR for dead letters.' },
        ],
      },
      {
        id: 'limits',
        title: '7. Known limits (0.1.1)',
        blocks: [
          {
            type: 'list',
            items: [
              'Subscriptions are at-most-once: no persistence, no replay; slow consumers drop events.',
              'Platform delivery is asynchronous and best-effort: accepted=true only means enqueued, not delivered.',
              'Admin-registered platforms disappear on restart.',
              'No web console and no delivery history query.',
              'Browsers need a gRPC-Web gateway to connect directly.',
            ],
          },
        ],
      },
      {
        id: 'checklist',
        title: '8. Three-minute checklist',
        blocks: [
          {
            type: 'list',
            ordered: true,
            items: [
              './scripts/gen-protos.sh java && gradle :server:installDist',
              'write a minimal config.yaml (no auth + local webhook)',
              'start the echo server (19800) and NotifyHub (9987)',
              'pip install -e sdks/python, then run examples/python/publish.py',
              'once the echo server prints JSON the path works → switch to real bots and enable auth.tokens',
            ],
          },
        ],
      },
    ],
  },

  config: {
    title: 'Configuration',
    desc: 'The server reads a YAML config. Precedence: --config argument > NOTIFYHUB_CONFIG env var > config.yaml in the working directory.',
    sections: [
      {
        id: 'server',
        title: 'server',
        blocks: [
          {
            type: 'table',
            head: ['Key', 'Default', 'Description'],
            rows: [
              ['host', '0.0.0.0', 'gRPC listen address'],
              ['port', '9987', 'gRPC listen port'],
              ['workers', 'CPU cores (min 4)', 'platform delivery worker threads'],
              ['queue_capacity', '10000', 'async delivery queue size; when full, Publish returns UNAVAILABLE'],
            ],
          },
        ],
      },
      {
        id: 'auth',
        title: 'auth',
        blocks: [
          { type: 'p', text: 'tokens: the API token allowlist.' },
          { type: 'note', text: 'An empty array disables authentication (only recommended for LAN / local debugging).' },
        ],
      },
      {
        id: 'defaults',
        title: 'defaults (global, overridable per platform)',
        blocks: [
          {
            type: 'table',
            head: ['Key', 'Default', 'Description'],
            rows: [
              ['retry.max_attempts', '3', 'max attempts for one delivery (including the first)'],
              ['retry.backoff_ms', '500', 'backoff base, exponential with jitter, single wait capped at 30s'],
              ['rate_limit.qps', '15', 'per-platform token bucket (DingTalk official limit is 20 qps)'],
              ['dedup_window_ms', '60000', 'dedup_key dedup window'],
            ],
          },
        ],
      },
      {
        id: 'platforms',
        title: 'platforms[] (push platforms)',
        blocks: [
          {
            type: 'table',
            head: ['Key', 'Description'],
            rows: [
              ['name', 'required, globally unique (duplicates fail startup)'],
              ['type', 'required: dingtalk / wecom / feishu / webhook'],
              ['webhook / url', 'required (either key), target address, http(s)://'],
              ['secret', 'signing key: SEC... for DingTalk/Feishu bots; HMAC key for the webhook channel'],
              ['topics', 'routing rules, wildcards * (one segment) / # (many); defaults to ["*"]'],
              ['template', 'optional placeholder template; defaults to title\\ncontent'],
              ['at_mobiles', 'optional. DingTalk @ phone numbers'],
              ['sign_header', 'optional (webhook). signature header name, defaults to X-Signature'],
              ['retry', 'optional {max_attempts, backoff_ms} override'],
              ['rate_limit_qps', 'optional. overrides the default qps'],
            ],
          },
        ],
      },
      {
        id: 'runtime',
        title: 'Runtime management',
        blocks: [
          { type: 'p', text: 'The Admin RPCs (UpsertPlatform / ListPlatforms / RemovePlatform) add, edit and remove platforms without a restart; platforms registered at runtime live in memory only, so the config file wins after a restart.' },
        ],
      },
    ],
  },

  protocol: {
    title: 'Protocol reference',
    desc: 'The contract is proto/notify/v1/notify.proto — the single source of truth. Field numbers and semantics are frozen.',
    sections: [
      {
        id: 'auth',
        title: 'Authentication',
        blocks: [
          {
            type: 'list',
            items: [
              'Once auth.tokens is configured, every method except Ping requires x-api-token: <token> in the metadata, otherwise UNAUTHENTICATED.',
              'An empty auth.tokens disables authentication.',
            ],
          },
        ],
      },
      {
        id: 'publish',
        title: 'Notify.Publish (unary)',
        blocks: [
          { type: 'p', text: 'Publish one notification: route it to platforms and broadcast it to online subscribers.' },
          {
            type: 'table',
            head: ['Field', 'Description'],
            rows: [
              ['topic', 'required. business topic, e.g. alert.db; empty returns INVALID_ARGUMENT'],
              ['title / content', 'notification title and body'],
              ['params', 'template variables map<string,string>'],
              ['platforms', 'explicit target platform names, overrides routing; unknown returns NOT_FOUND'],
              ['options.dedup_key', 'when non-empty enables dedup: repeats inside the window return accepted=false, deduplicated=true'],
              ['options.skip_subscribers', 'do not broadcast to subscribers'],
              ['options.skip_platforms', 'do not push to platforms; matched_platforms in the ack will be empty'],
            ],
          },
          { type: 'p', text: 'On success it returns PublishAck{event_id, accepted=true, matched_platforms} (the platforms actually queued). A full delivery queue returns UNAVAILABLE.' },
        ],
      },
      {
        id: 'subscribe',
        title: 'Notify.Subscribe (server stream)',
        blocks: [
          {
            type: 'list',
            items: [
              'topics support AMQP-style wildcards: * is exactly one segment, # is zero or more (dot-separated); # may only be the last segment.',
              'Semantics: at-most-once. Slow consumers drop events — no replay, no persistence.',
              'Disconnecting unsubscribes automatically.',
            ],
          },
        ],
      },
      {
        id: 'publish-stream',
        title: 'Notify.PublishStream (bidi stream)',
        blocks: [
          { type: 'p', text: 'Batch publish: one PublishAck per PublishRequest received. Difference from Publish: a per-message error (empty topic, unknown platform) does not break the stream — it returns an ack with accepted=false, error="...".' },
        ],
      },
      {
        id: 'admin',
        title: 'UpsertPlatform / ListPlatforms / RemovePlatform',
        blocks: [
          {
            type: 'list',
            items: [
              'UpsertPlatform validates name/type/webhook; type must be a registered channel (dingtalk | wecom | feishu | webhook), otherwise INVALID_ARGUMENT. Same name overwrites.',
              'Platform config takes effect at runtime and is never persisted — re-register after a restart or put it in the config file.',
              'RemovePlatform returns NOT_FOUND for unknown names.',
            ],
          },
        ],
      },
      {
        id: 'ping',
        title: 'Notify.Ping',
        blocks: [{ type: 'p', text: 'Health check, no authentication. Returns the version and uptime.' }],
      },
      {
        id: 'delivery',
        title: 'Delivery semantics (server → platform)',
        blocks: [
          {
            type: 'list',
            ordered: true,
            items: [
              'Publish returns the ack as soon as routing, dedup and enqueue are done; platform delivery happens asynchronously.',
              'Each platform gets its own token bucket (default 15 qps, configurable).',
              'Failures retry with exponential backoff (3 attempts by default, backoff_ms base + jitter, single wait capped at 30s).',
              'Final failure writes a dead-letter log and broadcasts on the built-in topic deadletter, with platform/topic/event_id/error in params.',
            ],
          },
        ],
      },
      {
        id: 'channels',
        title: 'Per-channel implementation details',
        blocks: [
          {
            type: 'table',
            head: ['Channel', 'Signing', 'Request', 'Success test'],
            rows: [
              ['dingtalk', 'urlencode(base64(HmacSHA256(key=secret, data=timestamp+"\\n"+secret))) appended to the URL', 'msgtype=markdown, supports at_mobiles', 'errcode==0'],
              ['wecom', 'none (key is in the URL)', 'msgtype=markdown, body limit ~2000 chars', 'errcode==0'],
              ['feishu', 'base64(HmacSHA256(key=timestamp+"\\n"+secret, data="")) in the body', 'msg_type=text', 'code==0 or StatusCode==0'],
              ['webhook', 'optional extra.sign_header (default X-Signature) = hex(HmacSHA256(key=secret, data=raw body))', 'JSON: {event_id, topic, title, content, params, timestamp}', 'HTTP 2xx'],
            ],
          },
          { type: 'p', text: 'Template placeholders: {{title}}, {{content}}, {{topic}}, {{event_id}}, {{params.xxx}} (or just {{xxx}}); unknown placeholders become empty.' },
        ],
      },
    ],
  },
}
