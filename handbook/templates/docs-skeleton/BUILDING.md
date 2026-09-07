# Building

## One blessed path

<Name exactly one supported build path and say why. Multiple documented paths
means none of them is tested. State that host builds are a convenience and CI
runs the container.>

## Prerequisites

| Requirement | Notes |
|---|---|
| Container engine | |
| Host tooling | <note PEP 668 distros: pipx or the standalone installer> |
| Disk | 100+ GiB free. The most common way a first build fails. |
| RAM | |

## Disk space

<Image size, why the caches are where they are, and the tune table -- machines
with different tunes share no target sstate, so the second board does not build
quickly because the first warmed the cache.>

## Building

```sh
export KAS_WORK_DIR=/path/with/space/<product>-build
export KAS_CONTAINER_ENGINE=docker
uv run kas-container build kas/machine/<board>.yml:kas/variant/<variant>.yml
```

<Explain how machine and variant compose, and that includes are repo-root-relative.>

## First build expectations

<Wall-clock time. Say plainly that a first build commonly fails once and that
this is normal. Tell them to use tmux or detach it -- closing the terminal kills
the build.>
