# Flags shared by Lagom (including Ladybird) and Serenity.
set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(CMAKE_COLOR_DIAGNOSTICS ON)

add_compile_options(-Wall)
add_compile_options(-Wextra)

add_compile_options(-Wno-address-of-packed-member)
add_compile_options(-Wcast-qual)
add_compile_options(-Wdeprecated-copy)
add_compile_options(-Wduplicated-cond)
add_compile_options(-Wformat=2)
add_compile_options(-Wimplicit-fallthrough)
add_compile_options(-Wlogical-op)
add_compile_options(-Wmisleading-indentation)
add_compile_options(-Wmissing-declarations)
add_compile_options(-Wnon-virtual-dtor)
add_compile_options(-Wsuggest-override)
add_compile_options(-Wundef)
add_compile_options(-Wunused)
add_compile_options(-Wwrite-strings)

add_compile_options(-Wno-invalid-offsetof)

add_compile_options(-Wno-unknown-warning-option)
add_compile_options(-Wno-unused-command-line-argument)

# This warning is triggered when one accepts or returns vectors from a function (that is not marked
# with [[gnu::target(...)]]) which would have been otherwise passed in register if the current
# translation unit had been compiled with more permissive flags wrt instruction selection (i. e. if
# one adds -mavx2 to cmdline). This will never be a problem for us since we (a) never use
# different instruction selection options across ABI boundaries; (b) most of the affected functions
# are actually TU-local.
#
# Moreover, even if we somehow properly annotated all of the SIMD helpers, calling them across ABI
# (or target) boundaries would still be very dangerous because of inconsistent and bogus handling of
# [[gnu::target(...)]] across compilers. See https://github.com/llvm/llvm-project/issues/64706 and
# https://www.reddit.com/r/cpp/comments/17qowl2/comment/k8j2odi .
add_compile_options(-Wno-psabi)

add_compile_options(-fno-exceptions)

add_compile_options(-ffp-contract=off)

if (CMAKE_CXX_COMPILER_ID STREQUAL "Clang" AND CMAKE_CXX_COMPILER_VERSION VERSION_GREATER_EQUAL "18")
    add_compile_options(-Wpadded-bitfield)
endif()

if (NOT CMAKE_HOST_SYSTEM_NAME MATCHES SerenityOS)
    # FIXME: Something makes this go crazy and flag unused variables that aren't flagged as such when building with the toolchain.
    #        Disable -Werror for now.
    add_compile_options(-Werror)
endif()

if (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    # Clang's default constexpr-steps limit is 1048576(2^20), GCC doesn't have one
    add_compile_options(-fconstexpr-steps=16777216)

    add_compile_options(-Wno-implicit-const-int-float-conversion)
    add_compile_options(-Wno-user-defined-literals)
    add_compile_options(-Wno-vla-cxx-extension)
    add_compile_options(-Wno-coroutine-missing-unhandled-exception)
elseif (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    # Only ignore expansion-to-defined for g++, clang's implementation doesn't complain about function-like macros
    add_compile_options(-Wno-expansion-to-defined)
    add_compile_options(-Wno-literal-suffix)

    # FIXME: This warning seems useful but has too many false positives with GCC 13.
    add_compile_options(-Wno-dangling-reference)
endif()

if (UNIX AND NOT APPLE AND NOT ENABLE_FUZZERS)
    add_compile_options(-fno-semantic-interposition)
    add_compile_options(-fvisibility-inlines-hidden)
endif()
