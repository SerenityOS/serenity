# Patches for glib on SerenityOS

## `0001-Use-glib-s-in-built-C_IN.patch`

Use glib's in-built C_IN

Since we do not have C_IN and glib has functionality for providing it,
let glib provide it.

## `0002-Let-glib-know-where-our-resolv.h-is-located.patch`

Let glib know where our 'resolv.h' is located


## `0003-Disable-IPV6-support.patch`

Disable IPV6 support

Serenity does not have IPV6 support so disable it

## `0004-Rename-glib-gio-mount-function-to-gio_mount.patch`

Rename glib/gio mount function to gio_mount

Somehow glib picks up on Serenity's mount function and gets confused

## `0005-Include-arpa-compatibility-definitions.patch`

Include arpa compatibility definitions

Serenity is missing all that is defined in this section so let's
include it.

## `0006-Add-stub-for-function-dn_expand.patch`

Add stub for function dn_expand.

Serenity is missing dn_expand so include a stub for it

## `0007-Include-strings.h-for-strcasecmp.patch`

Include 'strings.h' for strcasecmp


## `0008-Exclude-arpa-nameser.h-as-it-does-not-exist-on-Seren.patch`

Exclude arpa/nameser.h as it does not exist on Serenity


## `0009-Do-not-flag-support-for-extended-attributes-xattr.patch`

Do not flag support for extended attributes (xattr)


## `0010-Add-stub-for-g_unix_fd_query_path-on-SerenityOS.patch`

Add stub for g_unix_fd_query_path on SerenityOS


## `0011-Exclude-IPv6-tclass-message-support-on-SerenityOS.patch`

Exclude IPv6 tclass message support on SerenityOS


