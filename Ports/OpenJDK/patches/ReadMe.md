# Patches for OpenJDK on SerenityOS

## `0001-make-Add-Serenity-support-masquerading-as-BSD-when-n.patch`

make: Add Serenity support, masquerading as BSD when necessary


## `0002-make-Build-with-c-23-when-targeting-serenity.patch`

make: Build with c++23 when targeting serenity


## `0003-make-Remove-CUPS-dependency.patch`

make: Remove CUPS dependency


## `0004-hotspot-Add-workarounds-for-BSD-differences-from-ser.patch`

hotspot: Add workarounds for BSD differences from serenity

For the most part, we can pretend to be *BSD.

However, for some methods, we need to convince hotspot that we're macOS,
and others need serenity-specific ifdefs due to the lack of sysctl in
serenity.


## `0005-hotspot-Update-non-BSD-native-modules-for-Serenity.patch`

hotspot: Update non-BSD native modules for Serenity


## `0006-Add-serenity-specific-modules-to-java.base-and-jdk.a.patch`

Add serenity-specific modules to java.base and jdk.attach

It would be nice to re-direct the build to the same files *BSD use, but
for now we've got our own copy


## `0007-java.base-Update-native-modules-to-support-Serenity.patch`

java.base: Update native modules to support Serenity


## `0008-java.base-Enable-java.lang.Process-on-serenity.patch`

java.base: Enable java.lang.Process on serenity


