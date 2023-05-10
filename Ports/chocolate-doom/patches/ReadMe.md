# Patches for chocolate-doom on SerenityOS

## `0001-Remove-redundant-demoextend-definition.patch`

Remove redundant demoextend definition

GCC 10 enables -fno-common by default, which causes the linker to fail when
there are multple definitions of a global variable.

See https://gcc.gnu.org/gcc-10/porting_to.html

