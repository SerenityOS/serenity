# Flags shared by Lagom (including Ladybird) and Serenity.
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if (WIN32)
    # -Wall with clang-cl is equivalent to -Weverything, which is extremely noisy
    add_compile_options(-Wno-unknown-attributes) # [[no_unique_address]] is broken in MSVC ABI until next ABI break
    add_compile_options(-Wno-reinterpret-base-class)
    add_compile_options(-Wno-microsoft-unqualified-friend) # MSVC doesn't support unqualified friends
    add_compile_definitions(_CRT_SECURE_NO_WARNINGS) # _s replacements not desired (or implemented on any other platform other than VxWorks)
    add_compile_definitions(_CRT_NONSTDC_NO_WARNINGS) # POSIX names are just fine, thanks
    add_compile_definitions(_USE_MATH_DEFINES)
    add_compile_definitions(_WIN32_WINNT=0x0602)
    add_compile_definitions(NOMINMAX)
    add_compile_definitions(WIN32_LEAN_AND_MEAN)
    add_compile_definitions(NAME_MAX=255)
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
    add_compile_options(-Wno-deprecated-declarations)
endif()

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
    add_compile_options(-Wno-vla-cxx-extension)
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
