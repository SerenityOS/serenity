# Patches for ssmtp on SerenityOS

## `0001-Remove-_GNU_SOURCE-as-we-are-not-GNU.-With-it-we-seg.patch`

Remove `_GNU_SOURCE` as we are not GNU. With it we segfault after calling `basename()`.


## `0002-We-dont-have-u_int32_t-but-do-have-uint32_t-so-we-ty.patch`

We dont have `u_int32_t` but do have `uint32_t`, so we typedef to build successfull.


## `0003-Hardcode-paths-to-two-files-that-will-be-compiled-in.patch`

Hardcode paths to two files that will be compiled inside the binary. Otherwise it gets compiled with the hosts build path prepended.


## `0004-Use-generic-default-configuration.patch`

Use generic default configuration

Make the installation fully non-interactive and create a sane
default ssmtp.conf

