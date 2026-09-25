import { ICONS } from '../../data/icons.js'

export default {
  meta: {
    version: '0.1.1',
    repo: 'https://github.com/huangwenfu750/notifyhub',
    releases: 'https://github.com/huangwenfu750/notifyhub/releases',
    docs: 'https://github.com/huangwenfu750/notifyhub/tree/main/docs',
  },

  nav: {
    features: '特性',
    sdk: 'SDK',
    start: '快速开始',
    deploy: '部署',
    docs: '文档',
    github: 'GitHub',
  },

  lang: {
    label: '语言',
    zh: '中文',
    en: 'English',
    switchTo: '切换到 English',
  },

  theme: {
    label: '主题',
    system: '跟随系统',
    light: '日间模式',
    dark: '夜间模式',
    hint: '点击切换',
  },

  hero: {
    badgeVersion: 'v0.1.1',
    badgeLicense: 'MIT',
    badgeProto: 'proto3 + gRPC',
    title: '多语言通知推送服务',
    lead: '单进程部署，任意语言客户端通过 gRPC 接入；把通知推送到钉钉 / 企业微信 / 飞书 / 任意 Webhook，同时支持基于主题通配符的事件订阅。推送平台既能在 YAML 配置文件里声明，也能用代码在运行时配置。',
    ctaStart: '快速开始',
    ctaGithub: '在 GitHub 查看',
    platformsLabel: '投递到',
    terminalTitle: 'terminal — notifyhub',
    terminal: `$ ./server/build/install/server/bin/server --config config.yaml
[main] INFO io.notifyhub.Server - NotifyHub 0.1.1 已启动，监听 0.0.0.0:9987

$ client.publish("alert", "部署完成", "v1.2.0 上线")
  -> topic "alert" 命中 3 个平台
  -> ding-alert   ok   128ms
  -> wecom-ops    ok   96ms
  -> feishu-oncall queued (令牌桶等待 40ms)

$ client.subscribe(["alert.*"], handler)
  <- event alert.deploy  {"title":"部署完成"}`,
  },

  arch: {
    eyebrow: '架构',
    title: '一个进程，把消息从任意客户端送到任意平台',
    desc: '客户端只用 gRPC 说一件事：往某个 topic 发一条通知。路由、限流、重试、去重和平台签名都在服务端完成。',
    clientTitle: '客户端 SDK',
    serverTitle: 'NotifyHub Server',
    platformTitle: '推送平台',
    rpcs: [
      { name: 'Publish', desc: '推送到平台并广播事件' },
      { name: 'Subscribe', desc: '服务端流式接收订阅事件' },
      { name: 'Admin', desc: '运行时配置平台' },
    ],
    internals: ['队列', '重试', '限流', '去重', '死信'],
    clients: ['Java', 'Go', 'Python', 'JS / TS', 'Spring Boot'],
    platforms: ['钉钉机器人', '企业微信机器人', '飞书机器人', '通用 Webhook'],
    tagGrpc: 'gRPC · x-api-token',
    tagTopic: 'topic 通配',
  },

  features: {
    eyebrow: '特性',
    title: '为「把告警发出去」这一件事做扎实',
    desc: '不引入消息队列、不依赖数据库、不需要注册中心。能力集中在投递链路本身：路由、限流、重试、去重与死信。',
    items: [
      {
        icon: ICONS.globe,
        title: '多语言 SDK',
        desc: '协议为 proto3 + gRPC，官方 SDK 覆盖 Java、Python、TypeScript/JS、Go；任何支持 gRPC 的语言都能自己对接。',
        tags: ['Java', 'Python', 'TypeScript', 'Go'],
      },
      {
        icon: ICONS.bolt,
        title: 'Spring Boot 开箱即用',
        desc: '引入 notifyhub-spring-boot-starter，在 application.yml 里配好 host / port / token，注入 NotifyHubTemplate 即可推送。',
        tags: ['starter', '自动装配'],
      },
      {
        icon: ICONS.waves,
        title: '发布 / 订阅',
        desc: 'Publish 推送到平台并广播事件，Subscribe 以服务端流式实时接收；主题支持 * 与 # 通配，投递语义为至多一次。',
        tags: ['topic 通配', 'server streaming'],
      },
      {
        icon: ICONS.sliders,
        title: '推送平台即插即配',
        desc: '平台既能在 YAML 配置文件里静态声明，也能通过 Admin RPC 在运行时增删改，接入统一走 x-api-token 鉴权。',
        tags: ['YAML', 'Admin RPC', 'token'],
      },
      {
        icon: ICONS.shield,
        title: '生产化投递',
        desc: '每个平台独立令牌桶限流、指数退避重试、dedup_key 抑制告警风暴，失败到底进死信主题 deadletter。',
        tags: ['限流', '重试', '去重', '死信'],
      },
      {
        icon: ICONS.cube,
        title: '单进程，无外部依赖',
        desc: '一个进程、一份配置文件就跑起来；消息不落库、不重放，部署与排障的复杂度都压到最低。',
        tags: ['不持久化', '解压即用'],
      },
    ],
  },

  sdk: {
    eyebrow: 'SDK',
    title: '五种语言，一套调用',
    desc: '语义完全一致：连上服务、按 topic 发布、按需订阅。下面每段代码都能直接跑，只改地址和 token。',
    installLabel: '安装',
    tabs: [
      {
        id: 'java',
        label: 'Java',
        lang: 'java',
        install: 'implementation("io.github.huangwenfu750:sdk-java:0.1.1")',
        code: `// Java（sdks/java）
try (NotifyClient client = NotifyClient.newBuilder("localhost", 9987)
        .token("ntf_xxx").build()) {
    client.publish("alert", "部署完成", "v1.2.0 上线");
}`,
      },
      {
        id: 'python',
        label: 'Python',
        lang: 'python',
        install: 'pip install notifyhub-client',
        code: `# Python（sdks/python）
with NotifyClient("localhost:9987", token="ntf_xxx") as client:
    client.publish("alert", "部署完成", "v1.2.0 上线")

sub = client.subscribe(["alert.*"], print)   # 事件实时到达
sub.cancel()                                 # 退订`,
      },
      {
        id: 'ts',
        label: 'TypeScript',
        lang: 'ts',
        install: 'npm i notifyhub-client',
        code: `// TypeScript / JS（sdks/typescript）
const client = new NotifyClient("localhost:9987", "ntf_xxx");
await client.publish("alert", "部署完成", "v1.2.0 上线");`,
      },
      {
        id: 'go',
        label: 'Go',
        lang: 'go',
        install: 'go get github.com/huangwenfu750/notifyhub/sdks/go@v0.1.1',
        code: `// Go（sdks/go）
client, _ := notifyhub.New("localhost:9987", notifyhub.WithToken("ntf_xxx"))
client.Publish(ctx, "alert", "部署完成", "v1.2.0 上线")`,
      },
      {
        id: 'spring',
        label: 'Spring Boot',
        lang: 'java',
        install: 'implementation("io.github.huangwenfu750:notifyhub-spring-boot-starter:0.1.1")',
        code: `// Spring Boot（sdks/spring-boot）
// application.yml 配好 host / port / token，直接注入 NotifyHubTemplate
notify.publish("alert", "部署完成", "v1.2.0 上线");`,
      },
    ],
  },

  start: {
    eyebrow: '快速开始',
    title: '三步跑通第一条通知',
    desc: '从克隆仓库到钉钉群里收到消息，通常五分钟以内。',
    steps: [
      {
        n: '01',
        title: '启动服务端',
        desc: '生成 proto stub、构建发行包，复制配置模板填入机器人 webhook / secret 与 auth.tokens，然后启动。',
        lang: 'bash',
        code: `./scripts/gen-protos.sh java
gradle :server:installDist

cp config.example.yaml config.yaml   # 填入机器人 webhook / secret 与 auth.tokens

./server/build/install/server/bin/server --config config.yaml
# [main] INFO io.notifyhub.Server - NotifyHub 0.1.1 已启动，监听 0.0.0.0:9987`,
      },
      {
        n: '02',
        title: '发布一条通知',
        desc: '任选一种语言的 SDK，连上服务后按主题发布；平台按 topic 通配规则匹配并投递。',
        lang: 'python',
        code: `with NotifyClient("localhost:9987", token="ntf_xxx") as client:
    client.publish("alert", "部署完成", "v1.2.0 上线")`,
      },
      {
        n: '03',
        title: '运行时配置平台',
        desc: '不想改配置文件重启？用 Admin RPC 在运行时增删改推送平台。',
        lang: 'python',
        code: `client.upsert_platform(
    name="ding-alert", type="dingtalk",
    webhook="https://oapi.dingtalk.com/robot/send?access_token=xxx",
    secret="SECxxx", topics=["alert"],
)`,
      },
    ],
  },

  deploy: {
    eyebrow: '部署',
    title: '三种形态，任选其一',
    desc: '都是同一个进程。差别只在于你把构建放在本地、容器还是 CI 里。下拉查看全部三种方案 ——',
    cards: [
      {
        title: 'Docker 运行时镜像',
        desc: '把已构建的发行包塞进 JRE 基础镜像，不在容器里编译，小内存机器的首选。',
        lang: 'bash',
        code: `./scripts/package-linux.sh --no-jre
cp build/linux/notifyhub-0.1.1-linux-x86_64-nojre.tar.gz packaging/docker/
cp config.example.yaml packaging/docker/config.yaml
cd packaging/docker && docker compose up -d --build`,
      },
      {
        title: 'Linux 发行包',
        desc: '自带 Temurin JRE 21，解压即用无需预装 Java；一键安装到 /opt/notifyhub 并注册 systemd 服务。',
        lang: 'bash',
        code: `./scripts/package-linux.sh
tar -xzf notifyhub-0.1.1-linux-x86_64.tar.gz
sudo ./notifyhub-0.1.1-linux-x86_64/install.sh
sudo systemctl start notifyhub`,
      },
      {
        title: '源码构建',
        desc: '容器或 CI 内直接跑 Gradle 从源码构建，适合每次都要验证全链路的场景。',
        lang: 'bash',
        code: `cp config.example.yaml config.yaml
docker compose up --build -d

# 也可以只跑构建，自己起进程
./scripts/gen-protos.sh java && gradle :server:installDist`,
      },
    ],
  },

  install: {
    eyebrow: '安装',
    title: '各语言取包方式',
    desc: '服务端发行版在 Releases 提供自带 JRE 与精简版两种包，各带 .sha256 校验文件。',
    th: { lang: '语言', cmd: '安装', source: '来源' },
    copyHint: '复制安装命令',
    releasesNote: '服务端发行版：',
    releasesLink: 'GitHub Releases',
    rows: [
      { lang: 'Go', cmd: 'go get github.com/huangwenfu750/notifyhub/sdks/go@v0.1.1', note: '靠 sdks/go/v* 标签分发' },
      { lang: 'Java / Kotlin', cmd: 'implementation("io.github.huangwenfu750:sdk-java:0.1.1")', note: 'Maven Central' },
      { lang: 'Spring Boot', cmd: 'implementation("io.github.huangwenfu750:notifyhub-spring-boot-starter:0.1.1")', note: 'Maven Central' },
      { lang: 'TypeScript / JS', cmd: 'npm i notifyhub-client', note: 'npm' },
      { lang: 'Python', cmd: 'pip install notifyhub-client', note: 'PyPI' },
    ],
  },

  footer: {
    github: 'GitHub',
    releases: 'Releases',
    docs: '文档',
    license: 'MIT License',
  },

  decor: {
    title: '推送链路示意',
    tags: 'topic 通配  ·  限流  ·  重试  ·  去重  ·  死信',
  },
}
