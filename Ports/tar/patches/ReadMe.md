# Patches for tar on SerenityOS

## `0001-Fix-savannah-bug-64441.patch`

Release 1.35 has a bug in which it doesn't include libintl and libiconv
properly. Add the libiconv libraries to the build system.
