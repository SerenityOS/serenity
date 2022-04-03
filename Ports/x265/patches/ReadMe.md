# Patches for x265 on SerenityOS

## `posix_memalign.patch`

Patches x265 to not require posix_memalign but fallback to _aligned_malloc
