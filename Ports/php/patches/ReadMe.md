# Patches for php on SerenityOS

## `0001-Build-Disable-pharcmd.patch`

Build: Disable `pharcmd`

We do not support running the PHP binary locally after its build, so do
not try to run phar locally.

## `0002-Build-Force-inet_aton-detection.patch`

Build: Force `inet_aton` detection

For a reason unknown to me, the build system fails to find `inet_aton`
and tries to redefine it with its own implementation in
`flock_compat.c`.

