import { ICONS } from '../../data/icons.js'

export default {
  meta: {
    version: '0.1.1',
    repo: 'https://github.com/huangwenfu750/notifyhub',
    releases: 'https://github.com/huangwenfu750/notifyhub/releases',
    docs: 'https://github.com/huangwenfu750/notifyhub/tree/main/docs',
  },

  nav: {
    features: 'Features',
    sdk: 'SDK',
    start: 'Quick start',
    deploy: 'Deploy',
    docs: 'Docs',
    github: 'GitHub',
  },

  lang: {
    label: 'Language',
    zh: '中文',
    en: 'English',
    switchTo: 'Switch to 中文',
  },

  theme: {
    label: 'Theme',
    system: 'System',
    light: 'Light',
    dark: 'Dark',
    hint: 'click to switch',
  },

  hero: {
    badgeVersion: 'v0.1.1',
    badgeLicense: 'MIT',
    badgeProto: 'proto3 + gRPC',
    title: 'Multi-language notification push service',
    lead: 'Single-process deployment; clients in any language connect over gRPC. Notifications are pushed to DingTalk / WeCom / Feishu / arbitrary webhooks, plus topic-wildcard based event subscription. Push platforms are declared either in a YAML config file or configured at runtime from code.',
    ctaStart: 'Quick start',
    ctaGithub: 'View on GitHub',
    platformsLabel: 'Delivers to',
    terminalTitle: 'terminal — notifyhub',
    terminal: `$ ./server/build/install/server/bin/server --config config.yaml
[main] INFO io.notifyhub.Server - NotifyHub 0.1.1 started, listening on 0.0.0.0:9987

$ client.publish("alert", "Deploy finished", "v1.2.0 released")
  -> topic "alert" matched 3 platforms
  -> ding-alert   ok   128ms
  -> wecom-ops    ok   96ms
  -> feishu-oncall queued (token bucket wait 40ms)

$ client.subscribe(["alert.*"], handler)
  <- event alert.deploy  {"title":"Deploy finished"}`,
  },

  arch: {
    eyebrow: 'Architecture',
    title: 'One process, from any client to any platform',
    desc: 'Clients say just one thing over gRPC: publish a notification to a topic. Routing, rate limiting, retry, dedup and platform signing all happen server-side.',
    clientTitle: 'Client SDKs',
    serverTitle: 'NotifyHub Server',
    platformTitle: 'Push platforms',
    rpcs: [
      { name: 'Publish', desc: 'push to platforms and broadcast' },
      { name: 'Subscribe', desc: 'receive subscribed events as a server stream' },
      { name: 'Admin', desc: 'configure platforms at runtime' },
    ],
    internals: ['queue', 'retry', 'rate limit', 'dedup', 'dead letter'],
    clients: ['Java', 'Go', 'Python', 'JS / TS', 'Spring Boot'],
    platforms: ['DingTalk bot', 'WeCom bot', 'Feishu bot', 'Generic webhook'],
    tagGrpc: 'gRPC · x-api-token',
    tagTopic: 'topic wildcard',
  },

  features: {
    eyebrow: 'Features',
    title: 'Built to do one thing well: get the alert out',
    desc: 'No message queue, no database, no registry. All effort goes into the delivery path itself: routing, rate limiting, retry, dedup and dead-lettering.',
    items: [
      {
        icon: ICONS.globe,
        title: 'Multi-language SDKs',
        desc: 'proto3 over gRPC, with official SDKs for Java, Python, TypeScript/JS and Go; any language that speaks gRPC can talk to it directly.',
        tags: ['Java', 'Python', 'TypeScript', 'Go'],
      },
      {
        icon: ICONS.bolt,
        title: 'Spring Boot ready',
        desc: 'Add notifyhub-spring-boot-starter, set host / port / token in application.yml, inject NotifyHubTemplate and publish.',
        tags: ['starter', 'auto-config'],
      },
      {
        icon: ICONS.waves,
        title: 'Publish / subscribe',
        desc: 'Publish pushes to platforms and broadcasts the event; Subscribe receives a live server stream. Topics support * and # wildcards, delivery is at-most-once.',
        tags: ['topic wildcard', 'server streaming'],
      },
      {
        icon: ICONS.sliders,
        title: 'Pluggable push platforms',
        desc: 'Declare platforms statically in the YAML config, or add / edit / remove them at runtime through the Admin RPC — all behind x-api-token auth.',
        tags: ['YAML', 'Admin RPC', 'token'],
      },
      {
        icon: ICONS.shield,
        title: 'Production-grade delivery',
        desc: 'Per-platform token bucket rate limiting, exponential backoff retry, dedup_key to suppress alert storms, and a dead-letter topic when all else fails.',
        tags: ['rate limit', 'retry', 'dedup', 'dead letter'],
      },
      {
        icon: ICONS.cube,
        title: 'Single process, no dependencies',
        desc: 'One process and one config file is all it takes. Messages are never persisted or replayed, keeping deployment and debugging as simple as possible.',
        tags: ['no persistence', 'unpack and run'],
      },
    ],
  },

  sdk: {
    eyebrow: 'SDK',
    title: 'Five languages, one calling convention',
    desc: 'Identical semantics everywhere: connect, publish to a topic, subscribe when needed. Every snippet below runs as-is — just change host and token.',
    installLabel: 'Install',
    tabs: [
      {
        id: 'java',
        label: 'Java',
        lang: 'java',
        install: 'implementation("io.github.huangwenfu750:sdk-java:0.1.1")',
        code: `// Java (sdks/java)
try (NotifyClient client = NotifyClient.newBuilder("localhost", 9987)
        .token("ntf_xxx").build()) {
    client.publish("alert", "Deploy finished", "v1.2.0 released");
}`,
      },
      {
        id: 'python',
        label: 'Python',
        lang: 'python',
        install: 'pip install notifyhub-client',
        code: `# Python (sdks/python)
with NotifyClient("localhost:9987", token="ntf_xxx") as client:
    client.publish("alert", "Deploy finished", "v1.2.0 released")

sub = client.subscribe(["alert.*"], print)   # events arrive in real time
sub.cancel()                                 # unsubscribe`,
      },
      {
        id: 'ts',
        label: 'TypeScript',
        lang: 'ts',
        install: 'npm i notifyhub-client',
        code: `// TypeScript / JS (sdks/typescript)
const client = new NotifyClient("localhost:9987", "ntf_xxx");
await client.publish("alert", "Deploy finished", "v1.2.0 released");`,
      },
      {
        id: 'go',
        label: 'Go',
        lang: 'go',
        install: 'go get github.com/huangwenfu750/notifyhub/sdks/go@v0.1.1',
        code: `// Go (sdks/go)
client, _ := notifyhub.New("localhost:9987", notifyhub.WithToken("ntf_xxx"))
client.Publish(ctx, "alert", "Deploy finished", "v1.2.0 released")`,
      },
      {
        id: 'spring',
        label: 'Spring Boot',
        lang: 'java',
        install: 'implementation("io.github.huangwenfu750:notifyhub-spring-boot-starter:0.1.1")',
        code: `// Spring Boot (sdks/spring-boot)
// configure host / port / token in application.yml, then inject NotifyHubTemplate
notify.publish("alert", "Deploy finished", "v1.2.0 released");`,
      },
    ],
  },

  start: {
    eyebrow: 'Quick start',
    title: 'Three steps to your first notification',
    desc: 'From cloning the repo to a message landing in your DingTalk group — usually under five minutes.',
    steps: [
      {
        n: '01',
        title: 'Start the server',
        desc: 'Generate the proto stubs, build the distribution, copy the config template and fill in your bot webhook / secret plus auth.tokens, then start it.',
        lang: 'bash',
        code: `./scripts/gen-protos.sh java
gradle :server:installDist

cp config.example.yaml config.yaml   # fill in your bot webhook / secret and auth.tokens

./server/build/install/server/bin/server --config config.yaml
# [main] INFO io.notifyhub.Server - NotifyHub 0.1.1 started, listening on 0.0.0.0:9987`,
      },
      {
        n: '02',
        title: 'Publish a notification',
        desc: 'Pick any language SDK, connect, and publish to a topic; platforms are matched by topic wildcard and the message is delivered.',
        lang: 'python',
        code: `with NotifyClient("localhost:9987", token="ntf_xxx") as client:
    client.publish("alert", "Deploy finished", "v1.2.0 released")`,
      },
      {
        n: '03',
        title: 'Configure platforms at runtime',
        desc: 'Rather not edit the config file and restart? Use the Admin RPC to add, edit or remove push platforms at runtime.',
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
    eyebrow: 'Deploy',
    title: 'Three shapes, pick any one',
    desc: 'Same process in all cases. The only difference is where the build happens: locally, in a container, or in CI. Scroll to see all three —',
    cards: [
      {
        title: 'Docker runtime image',
        desc: 'Drops the prebuilt distribution into a JRE base image — nothing is compiled inside the container. Best pick for small machines.',
        lang: 'bash',
        code: `./scripts/package-linux.sh --no-jre
cp build/linux/notifyhub-0.1.1-linux-x86_64-nojre.tar.gz packaging/docker/
cp config.example.yaml packaging/docker/config.yaml
cd packaging/docker && docker compose up -d --build`,
      },
      {
        title: 'Linux distribution',
        desc: 'Bundles Temurin JRE 21 — unpack and run, no Java needed. One command installs to /opt/notifyhub and registers a systemd service.',
        lang: 'bash',
        code: `./scripts/package-linux.sh
tar -xzf notifyhub-0.1.1-linux-x86_64.tar.gz
sudo ./notifyhub-0.1.1-linux-x86_64/install.sh
sudo systemctl start notifyhub`,
      },
      {
        title: 'Build from source',
        desc: 'Run the Gradle build straight from source inside a container or CI — good when you want the full chain verified every time.',
        lang: 'bash',
        code: `cp config.example.yaml config.yaml
docker compose up --build -d

# or just build and start the process yourself
./scripts/gen-protos.sh java && gradle :server:installDist`,
      },
    ],
  },

  install: {
    eyebrow: 'Install',
    title: 'How to fetch the packages',
    desc: 'Server distributions ship in Releases in two flavours — bundled JRE and slim — each with a .sha256 checksum.',
    th: { lang: 'Language', cmd: 'Install', source: 'Source' },
    copyHint: 'Copy install command',
    releasesNote: 'Server distribution:',
    releasesLink: 'GitHub Releases',
    rows: [
      { lang: 'Go', cmd: 'go get github.com/huangwenfu750/notifyhub/sdks/go@v0.1.1', note: 'distributed via sdks/go/v* tags' },
      { lang: 'Java / Kotlin', cmd: 'implementation("io.github.huangwenfu750:sdk-java:0.1.1")', note: 'Maven Central' },
      { lang: 'Spring Boot', cmd: 'implementation("io.github.huangwenfu750:notifyhub-spring-boot-starter:0.1.1")', note: 'Maven Central' },
      { lang: 'TypeScript / JS', cmd: 'npm i notifyhub-client', note: 'npm' },
      { lang: 'Python', cmd: 'pip install notifyhub-client', note: 'PyPI' },
    ],
  },

  footer: {
    github: 'GitHub',
    releases: 'Releases',
    docs: 'Docs',
    license: 'MIT License',
  },

  decor: {
    title: '// leafer.js · delivery pipeline',
    tags: 'topic wildcard  ·  rate limit  ·  retry  ·  dedup  ·  dead letter',
  },
}
