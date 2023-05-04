# Patches for opusfile on SerenityOS

## `0001-http.c-Remove-include-ctype.h.patch`

Remove #include <ctype.h>

This simply removes the include statement for ctype.h. opusfile is not
very happy about our ctype.h because it contains static inline
definitions.

Also removing it does not hurt since we can compile fine without it.

