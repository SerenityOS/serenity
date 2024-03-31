# Patches for nesalizer on SerenityOS

## `0001-Add-Serenity-to-Makefile.patch`

Add Serenity to Makefile

- Add `-lSDL2 -lgui -lipc -lgfx -lcore -lcoreminimal -lpthread -lregex`
- Disable RTTI
- Add SDL2 include path to compile flags

## `0002-Disable-backtracing.patch`

Disable backtracing


