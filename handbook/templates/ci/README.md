# CI stubs

Two starting points. **Which platform Byte Lab standardises on is undecided**
-- keep both until it is settled, then delete one.

Both encode the same three stages, in this order for a reason:

1. **Guards** -- seconds, runs anywhere, fails fast. Catches leaked
   abstractions and unpinned repos before spending hours on a build.
2. **Build** -- every machine, release variant, artifacts published.
3. **Boot test** -- the emulated target only.

**A cold Yocto build needs ~100 GiB and several hours.** Hosted runners cannot
do it. Both files assume a self-hosted runner with a persistent cache volume,
and neither is useful without one.

Green CI means *the project builds and the emulated target boots*. It does not
mean the product boots on real silicon. Say so in a comment at the top of
whichever file you keep, so nobody reads a green tick as more than it is.
