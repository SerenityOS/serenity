# Patches for timidity on SerenityOS

## `0001-Calculate-Newton-coefficients-on-the-fly.patch`

Calculate Newton coefficients on the fly

Since we are cross-compiling, we do not get a binary we can run on the
host that is able to generate `newton_table.c`, so fall back to the old
way of generating the table.

