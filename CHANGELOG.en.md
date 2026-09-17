# Changelog

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Docs: the Maven Central section now spells out the full GPG procedure (generate a primary key only
  → send the public key to a keyserver → export the private key into a secret), plus a table of
  every secret the release pipelines need
- `.gitignore` excludes `*.asc` so an exported GPG private key can never be committed by accident

## [0.1.1] - 2026-09-17

### Added

- `scripts/check-versions.py`: the version number lives in 10 places — check or bump them all with
  one command (make it step zero of every release)
- Publication artifacts now include a `-javadoc.jar` (previously only `-sources.jar`), plus a new
  `gradle publishNotifyHubToCentralBundle` that produces a zip ready for Maven Central
- CI: three new jobs (Maven Central bundle, example project, Go module tag) and consistency checks
  for the version number and the license copies. The Go module tag `sdks/go/v*` is now created
  automatically instead of by hand

### Changed

- Publishing to GitHub Packages is now idempotent: it skips versions that already exist instead of
  failing with 409 and turning the Release red
- The Go SDK's `gen/` gRPC stubs are now committed (they used to be excluded by `.gitignore`), so
  the module compiles straight after `go get` — users no longer need a local protoc

### Fixed

- **The Go SDK v0.1.0 was unusable**: `.gitignore` excluded `sdks/go/gen/`, so the published module
  was missing `gen/notify/v1` and `go get` always failed with
  `cannot find module providing package ...`. The stubs are committed now — move to v0.1.1.
  The Go module proxy caches by version forever, so v0.1.0 cannot be fixed in place
- The npm / PyPI packages shipped license metadata but no LICENSE file; they do now
- Docs: state clearly that `io.github.huangwenfu750:*` is published to GitHub Packages only, not
  Maven Central, and add both the Maven `settings.xml` and the Gradle setup (only the root README
  had a Gradle snippet before)

## [0.1.0] - 2026-09-16

First release.

### Added

- gRPC server: wildcard topic routing, template rendering, deduplication, dead letters, rate
  limiting, and an Admin RPC for managing platforms at runtime
- Channels: DingTalk, WeCom, Feishu, generic webhook (signature algorithms in `docs/protocol.en.md`)
- Four SDKs: Java / Kotlin, Python, TypeScript / JavaScript, Go
- Spring Boot starter: auto-configures `NotifyClient` and `NotifyHubTemplate`, with a health
  indicator and Micrometer metrics
- Distribution: Linux tarballs (with / without a bundled JRE, each with a `.sha256`), a Docker
  image, and a systemd unit

### Notes

- Delivery is **at-most-once, nothing is persisted and nothing is replayed**: messages that were not
  delivered before a restart are not retried. Retry on the caller side if you need stronger
  guarantees.
