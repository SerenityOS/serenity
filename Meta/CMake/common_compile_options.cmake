set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if (MSVC)
    # -Wall with clang-cl is equivalent to -Weverything, which is extremely noisy
    add_compile_options(/W4)
else()
    add_compile_options(-Wall)
    add_compile_options(-Wextra)
endif()

if (NOT CMAKE_HOST_SYSTEM_NAME MATCHES SerenityOS)
    # FIXME: Something makes this go crazy and flag unused variables that aren't flagged as such when building with the toolchain.
    #        Disable -Werror for now.
    add_compile_options(-Werror)
endif()
