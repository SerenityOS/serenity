# Patches for glib (and submodules) on SerenityOS

## `0001-poll.h-is-located-at-root.patch`

glib includes poll.h from sys/poll.h, but our poll.h is located at root.

### Status
- [ ] Local?
- [X] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0002-use-glib-in-built-C_IN.patch`

We do not have C_IN so use glib's in-built C_IN

### Status
- [X] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `0003-let-glib-know-where-our-resolv.h-is.patch`

Let glib's res_query_test know where our resolv.h is located.

### Status
- [ ] Local?
- [X] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0004-disable-IPV6-support.patch`

Disable IPV6 support since we do not support that yet.

### Status
- [X] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `0005-serenity-does-not-have-IN_MULTICAST.patch`

Since Serenity does not have IN_MULTICAST we just return 0 instead of calling IN_MULTICAST.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [X] Hack

## `0006-conflict-rename-gio-mount-function.patch`

Somehow we get a conflict with glib's mount function. This patch renames glib's mount function to gio_mount.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [X] Hack

## `0009-include-section-with-missing-functionality.patch`

This includes a bigger section with functionality that Serenity is missing.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `0010-stub-for-function-dn_expand.patch`

Adds a stub for the function dn_expand.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [X] Hack

## `0011-ntohl-ntohs-located-in-arpa-inet.h.patch`

In Serenity ntohl/ntohs is located in arpa/inet.h, other stuff glib needs is included in 'netinet/in.h'.

### Status
- [ ] Local?
- [X] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `0012-include-strings.h-for-strcasecmp.patch`

Include 'strings.h' for strcasecmp.

### Status
- [ ] Local?
- [X] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `0013-nameser.h-is-not-needed.patch`

glib compiles fine without arpa/nameser.h so do not include since we do not yet support it.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [X] Hack
