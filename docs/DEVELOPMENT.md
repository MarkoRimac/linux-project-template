# Development

Everything here runs inside the build container:

```sh
uv run kas-container shell kas/machine/<board>.yml
```

## Why is this package in my image?

```sh
bitbake -g bytelab-image
oe-depends-dot --why --key <package> task-depends.dot
```

Indispensable when an image is bigger than expected. `bitbake -g` writes
`task-depends.dot` into the current directory; `oe-depends-dot` explains the
chain that pulled something in.

## Working on a recipe's source

```sh
devtool modify <recipe>            # clone into workspace, patch-aware
# edit sources under build/workspace/sources/<recipe>
devtool build <recipe>
devtool deploy-target <recipe> root@<ip>
```

`devtool deploy-target` needs free space on the target, which is why
`bytelab-image-debug` reserves an extra 128 MiB.

When finished:

```sh
devtool finish <recipe> meta-bytelab-product      # or reset, to discard
```

## Debugging on target from VS Code

```sh
devtool ide-sdk <recipe> bytelab-image-debug --target root@<ip> --ide=code
```

This generates a VS Code configuration with a working cross toolchain and
`gdbserver` attach. It requires the debug image, which is what
`IMAGE_GEN_DEBUGFS`, `image-combined-dbg` and `tools-debug` in
`bytelab-image-debug.bb` are for.

## System-wide tracing

```sh
crosstap -r root@<ip> -s script.stp
```

SystemTap against the target, using the debug image's symbols. Useful for
watching a driver ioctl path without adding printks.

## Inspecting the final value of a variable

```sh
bitbake -e bytelab-image | grep -E '^IMAGE_FSTYPES=|^SERIAL_CONSOLES=|^DISTRO_FEATURES='
```

The single most useful command when a machine conf is not doing what you think.
`bitbake -e <recipe>` shows the fully expanded configuration with the file and
line each assignment came from.

## Cleaning one recipe

```sh
bitbake -c cleansstate <recipe>
```

Note `kas/base.yml` sets `INHERIT += "rm_work"`, so per-recipe work trees are
deleted as the build proceeds. Use `devtool modify` when you need to inspect
sources rather than disabling `rm_work` globally.
