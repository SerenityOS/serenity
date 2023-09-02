# Patches for acpica-tools on SerenityOS

## `0001-Add-serenity-definitions-for-LibC-includes.patch`

Add serenity definitions for LibC includes

We use the netbsd "acnetbsd.h" file here as a template.

## `0002-Ignore-unknown-warning-options.patch`

Ignore unknown warning options

`-Wlogical-op`, `-Wmissing-parameter-type`, and `-Wold-style-declaration`
are unknown to Clang. While this isn't fatal by itself, it is very
noisy.

