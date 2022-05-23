# Patches for glib on SerenityOS

## `0001-poll.h-is-located-at-root.patch`

meson.build: 'poll.h' is located at root, not 'sys/poll.h'


## `0002-use-glib-in-built-C_IN.patch`

gio/meson.build: Use glib's in-built C_IN

Since we do not have C_IN and glib has functionality for providing it,
let glib provide it.

## `0003-let-glib-know-where-our-resolv.h-is.patch`

gio/meson.build: Let glib know where our 'resolv.h' is located


## `0004-disable-IPV6-support.patch`

meson.build: Disable IPV6 support

Serenity does not have IPV6 support so disable it

## `0005-serenity-does-not-have-IN_MULTICAST.patch`

gio/ginetaddress.c: Serenity does not have IN_MULTICAST, just return 0

Since Serenity does not have IN_MULTICAST we just return 0

## `0006-conflict-rename-gio-mount-function.patch`

gio/gio-tool-mount.c: Rename glib/gio mount function to gio_mount

Somehow glib picks up on Serenity's mount function and gets confused

## `0009-include-section-with-missing-functionality.patch`

gio/gthredresolver.c: Need to include this section

Serenity is missing all that is defined in this section so let's
include it.

## `0010-stub-for-function-dn_expand.patch`

gio/gthreadedresolver.c: Add stub for function dn_expand.

Serenity is missing dn_expand so include a stub for it

## `0011-ntohl-ntohs-located-in-arpa-inet.h.patch`

gio/xdgmime/xdgmimecache.c: ntohl/ntohs is located in 'arpa/inet.h'

In Serenity ntohl/ntohs is located in arpa/inet.h, other stuff glib
needs is included in 'netinet/in.h'.

## `0012-include-strings.h-for-strcasecmp.patch`

Include 'strings.h' for strcasecmp


## `0013-nameser.h-is-not-needed.patch`

arpa/nameser.h is not needed, and Serenity does not have it at the moment.


