# Patches for acpica-tools on SerenityOS

## `0001-Stop-compiler-warnings-on-dangling-pointer.patch`

Stop compiler warnings on dangling pointer


## `0002-Add-serenity-definitions-for-LibC-includes.patch`

Add serenity definitions for LibC includes

We use the netbsd "acnetbsd.h" file here as a template.

## `0003-Disable-warnings-for-Werror-bad-function-cast.patch`

Disable warnings for Werror=bad-function-cast


## `0004-Remove-unsupported-warning-flags-for-cross-compile.patch`

Remove unsupported warning flags for cross-compile

* -Wlogical-op
* -Wmissing-parameter-type
* -Wold-style-declaration

