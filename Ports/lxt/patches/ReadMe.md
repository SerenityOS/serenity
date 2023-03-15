# Patches for lxt on SerenityOS

## `0001-Add-support-for-SerenityOS.patch`

This patch adds support for SerenityOS.

Specifially, it does the following things:
1. Specifies MAXNAMLEN, otherwise it's undefined and build fails
2. Addresses issues with configure.ac
3. Fixes path to clear utility


