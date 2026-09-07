# Architecture

## Layer stack

```
<layer>   prio <n>   <what it owns>
...
```

<State why Byte Lab layers sit above vendor BSP layers. Vendor layers are
commonly BBFILE_PRIORITY 9; anything below that loses override contests
silently. Say what the reusable BSP layer may depend on -- if it depends on a
vendor layer it is no longer reusable.>

## Boot chain

<An ASCII diagram of the real product target, stage by stage, naming which
stages are closed vendor blobs and who supplies them.>

<If a second board boots differently, add a table of what it does instead and
state plainly what it therefore cannot teach. A non-product validation board
should never be the source of the boot-chain documentation.>

| GPT entry / partition | Contents |
|---|---|

## Partition layout and first boot

<How the image is composed, and the first-boot expansion mechanism if any. Call
out the non-obvious dependency: systemd only treats a boot as the first boot if
/etc/machine-id is empty in the image.>
