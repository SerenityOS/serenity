## Build Guide

- Run `./package.sh` here, which should install the dependencies and copy the patched source code to `~anon/Source/cmake`
- Give the vm at least 2G of RAM, and at least 1.5G of free disk space (actual values used may be different, but ~1.3GiB of RAM and 1.2GiB of disk space has been observed)
- Build the bootstrap cmake binary:
```sh
$ cd Source/cmake
$ ./bootstrap
```
- Go entertain yourself for a few minutes (build takes about 10m)
- The bootstrap binary should be built, but may fail for any of the reasons detailed in [Troubleshooting](#troubleshooting).
- Build and install cmake (this takes a long time, about 80 minutes on my machine)
```sh
$ make
# mount -o bind /usr /usr
# make install
```
- Optionally, add `/usr/local/bin` to PATH:
```sh
$ export PATH="$PATH:/usr/local/bin"
```
- Hopefully look back at the experience and laugh at its ease

## Current Status

Fully working :P

## Troubleshooting

### "Failed to open check cache file for write..." while bootstrap cmake is configuring cmake

The cause for this is unknown, but it seems to be a transitive state; simply restarting the bootstrap process fixes it:
```sh
$ Bootstrap.cmk/cmake . -C Bootstrap.cmk/InitialCacheFlags.cmake -G 'Unix Makefiles' -DCMAKE_BOOTSTRAP=1 -DBUILD_TESTING=0
```
You might have to repeat this many times.

### "unable to rename '...', reason: File already exists" while making cmake

Cause unknown. re-running make made it go away.
