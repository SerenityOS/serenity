# Patches for gcc on SerenityOS

## `0001-Add-a-gcc-driver-for-SerenityOS.patch`

Add a gcc driver for SerenityOS

This patch adds support for the `*-*-serenity` target to gcc.

It specifies which flags need to be passed to the linker, defines the
__serenity__ macro, sets the correct underlying type of `size_t` and
`ptrdiff_t`, and enables IFUNCs.


## `0002-fixincludes-Skip-for-SerenityOS-targets.patch`

fixincludes: Skip for SerenityOS targets

`fixincludes` is responsible for fixing mistakes in system headers that
rely in compiler extensions that GCC doesn't support or cause errors in
C++ mode.

Our headers don't have such problems, so this hack is of no use for us.

## `0003-libgcc-Build-for-SerenityOS.patch`

libgcc: Build for SerenityOS

This patch enables building gcc's own C runtime files, and sets up
exception handling support.


## `0004-libgcc-Do-not-link-libgcc_s-to-LibC.patch`

libgcc: Do not link libgcc_s to LibC

The toolchain is built before LibC, so linking to the C runtime library
would fail.


## `0005-i386-Disable-math-errno-for-SerenityOS.patch`

i386: Disable math errno for SerenityOS

SerenityOS uses exceptions for math error handling, which allows the
compiler to do more optimizations on calls to math functions. This patch
has the effect of setting -fno-math-errno by default.

## `0006-libstdc-Support-SerenityOS.patch`

libstdc++: Support SerenityOS

During the toolchain build, SerenityOS libraries are not available, so
we have to manually tell libstdc++ about what our LibC supports.

In most places, we take the Newlib code paths.


## `0007-libstdc-Build-static-library-with-fPIC.patch`

libstdc++: Build static library with -fPIC

We want the libstdc++.a library to contain -fPIC code in order to link
it statically into LibC/our shared objects. However, the build system
forces no-pic/pie instead.

This hack is from https://gcc.gnu.org/bugzilla/show_bug.cgi?id=58638

## `0008-Include-safe-ctype.h-after-C-standard-headers-to-avo.patch`

Include safe-ctype.h after C++ standard headers, to avoid over-poisoning

When building gcc's C++ sources against recent libc++, the poisoning of
the ctype macros due to including safe-ctype.h before including C++
standard headers such as <list>, <map>, etc, causes many compilation
errors, similar to:

  In file included from /home/dim/src/gcc/master/gcc/gensupport.cc:23:
  In file included from /home/dim/src/gcc/master/gcc/system.h:233:
  In file included from /usr/include/c++/v1/vector:321:
  In file included from
  /usr/include/c++/v1/__format/formatter_bool.h:20:
  In file included from
  /usr/include/c++/v1/__format/formatter_integral.h:32:
  In file included from /usr/include/c++/v1/locale:202:
  /usr/include/c++/v1/__locale:546:5: error: '__abi_tag__' attribute
  only applies to structs, variables, functions, and namespaces
    546 |     _LIBCPP_INLINE_VISIBILITY
        |     ^
  /usr/include/c++/v1/__config:813:37: note: expanded from macro
  '_LIBCPP_INLINE_VISIBILITY'
    813 | #  define _LIBCPP_INLINE_VISIBILITY _LIBCPP_HIDE_FROM_ABI
        |                                     ^
  /usr/include/c++/v1/__config:792:26: note: expanded from macro
  '_LIBCPP_HIDE_FROM_ABI'
    792 |
    __attribute__((__abi_tag__(_LIBCPP_TOSTRING(
  _LIBCPP_VERSIONED_IDENTIFIER))))
        |                          ^
  In file included from /home/dim/src/gcc/master/gcc/gensupport.cc:23:
  In file included from /home/dim/src/gcc/master/gcc/system.h:233:
  In file included from /usr/include/c++/v1/vector:321:
  In file included from
  /usr/include/c++/v1/__format/formatter_bool.h:20:
  In file included from
  /usr/include/c++/v1/__format/formatter_integral.h:32:
  In file included from /usr/include/c++/v1/locale:202:
  /usr/include/c++/v1/__locale:547:37: error: expected ';' at end of
  declaration list
    547 |     char_type toupper(char_type __c) const
        |                                     ^
  /usr/include/c++/v1/__locale:553:48: error: too many arguments
  provided to function-like macro invocation
    553 |     const char_type* toupper(char_type* __low, const
    char_type* __high) const
        |                                                ^
  /home/dim/src/gcc/master/gcc/../include/safe-ctype.h:146:9: note:
  macro 'toupper' defined here
    146 | #define toupper(c) do_not_use_toupper_with_safe_ctype
        |         ^

This is because libc++ uses different transitive includes than
libstdc++, and some of those transitive includes pull in various ctype
declarations (typically via <locale>).

There was already a special case for including <string> before
safe-ctype.h, so move the rest of the C++ standard header includes to
the same location, to fix the problem.

gcc/ChangeLog:

	* system.h: Include safe-ctype.h after C++ standard headers.

Signed-off-by: Dimitry Andric <dimitry@andric.com>
(cherry picked from commit 9970b576b7e4ae337af1268395ff221348c4b34a)

