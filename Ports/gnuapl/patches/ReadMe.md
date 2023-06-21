# Patches for gnuapl on SerenityOS

## `0001-Include-fcntl-find-fcntl.h.patch`

Include fcntl find fcntl.h

`fcntl.h` was included as `sys/fcntl.h`, which is not where this lives in Serenity.

Also `sys/select.h` is included here.

## `0002-Stub-out-the-performance-report-macro.patch`

Stub out the performance report macro

The Macro for performance reporting was throwing compile errors, so we just stub it out.

## `0003-Remove-use-of-sbrk.patch`

Remove use of sbrk()

Again, for performance reporting the function `sbrk` is needed which we don't have. We just stub it out.

