# Patches for LuaRocks on SerenityOS

## `luarocks.patch`

src/luarocks/core/cfg.lua: Setup serenity as a platform and config `defaults.variables.UNZIP` to work around our `unzip` command not supporting the flag `-n`
src/luarocks/core/sysdetect.lua: Detect SerenityOS using the `uname` command

This should should be upstreamed so next versions of LuaRocks will support Serenity out of the box.
