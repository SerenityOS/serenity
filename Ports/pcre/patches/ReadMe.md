# Patches for pcre on SerenityOS

## `0001-test-Disable-S-on-serenity.patch`

test: Disable '-S' on serenity

This flag uses setrlimit(), which is not supported.

