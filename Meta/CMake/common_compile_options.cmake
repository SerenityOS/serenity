# Flags shared by Lagom (including Ladybird) and Serenity.
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_compile_options(-Wall)
add_compile_options(-Wextra)

add_compile_options(-Wno-unknown-warning-option)
add_compile_options(-Wno-unused-command-line-argument)

add_compile_options(-fdiagnostics-color=always)
add_compile_options(-fno-exceptions)

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
