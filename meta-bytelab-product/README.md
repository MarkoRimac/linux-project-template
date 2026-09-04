# meta-bytelab-product

The product layer. **Rename this to `meta-<product>` when starting a real
project** and change `BBFILE_COLLECTIONS`, `BBFILE_PRIORITY_*`, `LAYERDEPENDS_*`
and `LAYERSERIES_COMPAT_*` in `conf/layer.conf` to match.

- **Priority:** 11, above `meta-bytelab-bsp` (10) and every vendor layer (9), so
  a product can override anything below it.
- **Depends on:** `core`, `meta-bytelab-bsp`.

## Contents

| Path | What |
|---|---|
| `conf/distro/bytelab.conf` | The distro. Machine-neutral by construction |
| `recipes-core/images/bytelab-image.bb` | Release image |
| `recipes-core/images/bytelab-image-debug.bb` | Development image. Never ship it |
| `recipes-example/hello-bytelab/` | One application recipe to copy |

## Rules for this layer

1. **The distro conf may not assume a machine.** No `SERIAL_CONSOLES`, no
   `IMAGE_FSTYPES`, no `KERNEL_DEVICETREE`, no `LICENSE_FLAGS_ACCEPTED`. Those
   live in the machine conf or the kas variant that needs them.
2. **`bytelab-image-debug.bb` must stay a strict superset** of
   `bytelab-image.bb` via `require`, so the two cannot drift.
3. Product application recipes go in `recipes-<area>/`, following the
   `hello-bytelab` pattern.
