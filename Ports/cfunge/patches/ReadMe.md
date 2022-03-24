# Patches for cfunge on SerenityOS

## `arc4random_buf.patch`
Somewhere on the way of configuring the variable `HAVE_arc4random_buf` was set which lead to the linker complaining about not knowing a certain `arc4random_stir()` function.
This patch just negates the define and the linker is happy.


## `define-max.patch`
It is expected that `sys/param.h` defines a `MAX` macro. We don't. So here the needed macro is just inserted instead of the include.


## `posix-mapped-files.patch`
It is expected that `_POSIX_MAPPED_FILES` is defined as at least `1`, so we do that here.


## `posix-regexp.patch`
Same as before, just for `_POSIX_REGEXP`


