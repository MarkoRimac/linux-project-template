# Development

Everything here runs inside the build container:

```sh
uv run kas-container shell kas/machine/<board>.yml
```

## Why is this package in my image?

```sh
bitbake -g <image>
oe-depends-dot --why --key <package> task-depends.dot
```

## Iterating on a recipe without reflashing

```sh
devtool modify <recipe>
devtool build <recipe>
devtool deploy-target <recipe> root@<ip>
devtool finish <recipe> meta-<product>   # or reset, to discard
```

## On-target debugging

```sh
devtool ide-sdk <recipe> <debug-image> --target root@<ip> --ide=code
```

## Inspecting the final value of a variable

```sh
bitbake -e <image> | grep -E '^DISTRO_FEATURES=|^SERIAL_CONSOLES='
```

<The single most useful command when a machine conf is not doing what you think.
`bitbake -e` shows every assignment with the file and line it came from.>
