# Patches for glib on SerenityOS

## `0001-poll.h-is-located-at-root-not-sys-poll.h.patch`

'poll.h' is located at root, not 'sys/poll.h'


## `0002-Use-glib-s-in-built-C_IN.patch`

Use glib's in-built C_IN

Since we do not have C_IN and glib has functionality for providing it,
let glib provide it.

## `0003-Let-glib-know-where-our-resolv.h-is-located.patch`

Let glib know where our 'resolv.h' is located


## `0004-Disable-IPV6-support.patch`

Disable IPV6 support

Serenity does not have IPV6 support so disable it

## `0005-Serenity-does-not-have-IN_MULTICAST-just-return-0.patch`

Serenity does not have IN_MULTICAST, just return 0

Since Serenity does not have IN_MULTICAST we just return 0

## `0006-Rename-glib-gio-mount-function-to-gio_mount.patch`

Rename glib/gio mount function to gio_mount

Somehow glib picks up on Serenity's mount function and gets confused

## `0007-Include-arpa-compatibility-definitions.patch`

Include arpa compatibility definitions

Serenity is missing all that is defined in this section so let's
include it.

## `0008-Add-stub-for-function-dn_expand.patch`

Add stub for function dn_expand.

Serenity is missing dn_expand so include a stub for it

## `0009-ntohl-ntohs-is-located-in-arpa-inet.h.patch`

ntohl/ntohs is located in 'arpa/inet.h'

In Serenity ntohl/ntohs is located in arpa/inet.h, other stuff glib
needs is included in 'netinet/in.h'.

## `0010-Include-strings.h-for-strcasecmp.patch`

Include 'strings.h' for strcasecmp


## `0011-Exclude-arpa-nameser.h-as-it-does-not-exist-on-Seren.patch`

Exclude arpa/nameser.h as it does not exist on Serenity


## `0012-Do-not-flag-support-for-extended-attributes-xattr.patch`

Do not flag support for extended attributes (xattr)


