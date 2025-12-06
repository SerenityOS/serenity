# Patches for php on SerenityOS

## `0001-Build-Disable-pharcmd.patch`

Build: Disable `pharcmd`

We do not support running the PHP binary locally after its build, so do
not try to run phar locally.

## `0002-Build-Patch-Serenity-root-directory-into-libtool.patch`

Build: Patch Serenity root directory into libtool

PHP's libtool does not have sysroot support; this is the minimum change
to get PHP to build.

## `0003-Remove-include-of-sys-ipc.h.patch`

Remove include of sys/ipc.h


## `0004-Disable-unsupported-prctl-call.patch`

Disable unsupported prctl call

Serenity has prctl but doesn't define PR_SET_PDEATHSIG.

