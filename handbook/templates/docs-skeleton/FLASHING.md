# Flashing

<One section per board. State clearly which procedures have been *run* and which
are derived from another project and not yet validated.>

## <board>

**Status: <validated on hardware | not yet validated>.**

<Recovery/download mode entry, per board revision. The button, the jumper, the
pad to short. This is the section people read at 11pm.>

```sh
<the actual commands, with the device-identification step first>
```

> **Closed blobs.** <Name every closed binary in the flashing path, where it came
> from, and what it encodes. A DDR training blob is board-specific: the vendor's
> will not boot a board that deviates from the reference design. Treat provenance
> as a supply-chain question.>

Console: **<baud> 8N1 on `<tty>`**.

## After first boot, on any board

```sh
findmnt /
df -h /
```

<What to check if the rootfs did not grow.>
