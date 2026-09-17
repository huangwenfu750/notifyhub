# Changelog

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- `scripts/check-versions.py`: the version number lives in 10 places — check or bump them all with
  one command (make it step zero of every release)
- Publication artifacts now include a `-javadoc.jar` (previously only `-sources.jar`), plus a new
  `gradle publishNotifyHubToCentralBundle` that produces a zip ready for Maven Central
- CI: two new jobs (Maven Central bundle, example project) and consistency checks for the version
  number and the license copies

### Changed

- Publishing to GitHub Packages is now idempotent: it skips versions that already exist instead of
  failing with 409 and turning the Release red

### Fixed

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
