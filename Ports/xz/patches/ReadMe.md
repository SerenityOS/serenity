# Patches for xz on SerenityOS

## `0001-Remove-unsupported-RLIM_SAVED_-usage.patch`

Remove unsupported RLIM_SAVED_* usage


## `0002-liblzma-Don-t-assume-getauxval-is-Linux-only.patch`

liblzma: Don't assume getauxval is Linux-only

SerenityOS doesn't expose Linux-compatible hwcaps (like HWCAP_CRC32)
in the auxiliary vector.

