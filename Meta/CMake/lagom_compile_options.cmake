include(${CMAKE_CURRENT_LIST_DIR}/common_compile_options.cmake)

add_compile_options(-Wno-implicit-const-int-float-conversion)
add_compile_options(-Wno-literal-suffix)
add_compile_options(-Wno-maybe-uninitialized)
add_compile_options(-Wno-unknown-warning-option)
add_compile_options(-Wno-unused-command-line-argument)

if (NOT ENABLE_FUZZERS AND NOT APPLE AND NOT WIN32)
    add_compile_options(-fno-semantic-interposition)
endif()

if (NOT WIN32)
    add_compile_options(-fPIC)
endif()

if (WIN32)
    add_compile_options(-Wno-unknown-attributes) # [[no_unique_address]] is broken in MSVC ABI until next ABI break
    add_compile_definitions(_CRT_SECURE_NO_WARNINGS) # _s replacements not desired (or implemented on any other platform other than VxWorks)
    add_compile_definitions(_CRT_NONSTDC_NO_WARNINGS) # POSIX names are just fine, thanks
    add_compile_definitions(NOMINMAX)
    add_compile_definitions(WIN32_LEAN_AND_MEAN)
    add_compile_definitions(NAME_MAX=255)
endif()

if (NOT MSVC)
    add_compile_options(-fsigned-char)
    add_compile_options(-fno-exceptions)
    add_compile_options(-fdiagnostics-color=always)
    add_compile_options(-g1)
    add_compile_options(-O2)
endif()
