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
