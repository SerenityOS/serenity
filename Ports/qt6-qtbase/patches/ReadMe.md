# Patches for qt6-qtbase on SerenityOS

## `0001-Add-a-SerenityOS-platform-definition.patch`

Add a SerenityOS platform definition


## `0002-Disable-shared-memory-and-semaphores.patch`

Disable shared memory and semaphores

It's probably not done in the cleanest way but it works

## `0003-Serenity-doesn-t-support-utimensat-and-UTIME_NOW.patch`

Serenity doesn't support utimensat and UTIME_NOW


## `0004-Hack-Force-searching-for-plugins-in-usr-local.patch`

Hack: Force searching for plugins in /usr/local

I really don't know how else to do this but I'm sure there is a proper
way to handle this. But this works and doesn't break the system so
whatever for now.

## `0005-Disable-version-tagging.patch`

Disable version tagging

This is necessary because as of now, Serenity doesn't support DT_VERSYM
and other related ELF objects

## `0006-Serenity-Disable-local-domain-name-lookup-via-resolv.patch`

Serenity: Disable local domain name lookup via resolv

Serenity doesn't support /etc/resolv.conf or the structures in
<resolv.h> to do Unix-like domain name resolution.

## `0007-Disable-QDnsLookup-entirely.patch`

Disable QDnsLookup entirely

It seems that we used to include the host's system headers, which we no
longer do. Now this feature breaks the package, which is why we have to
disable it in order to build Qt6.

