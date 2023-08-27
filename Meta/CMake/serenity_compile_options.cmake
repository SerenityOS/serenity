include(${CMAKE_CURRENT_LIST_DIR}/common_compile_options.cmake)

# The following options are sorted by the "base" name (the part excluding the initial Wno/fno or W/f).

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

add_compile_options(-fno-delete-null-pointer-checks)
add_compile_options(-ffile-prefix-map=${SerenityOS_SOURCE_DIR}=.)
add_compile_options(-fno-omit-frame-pointer)
add_compile_options(-fsized-deallocation)
add_compile_options(-fstack-clash-protection)
add_compile_options(-fstack-protector-strong)
add_link_options(-fstack-protector-strong)

add_compile_options(-g1)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    add_compile_options(-Wno-maybe-uninitialized)
    add_compile_options(-Wcast-align)
    add_compile_options(-Wdouble-promotion)
elseif (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    add_compile_options(-Wno-atomic-alignment)
    add_compile_options(-Wno-unused-const-variable)
    add_compile_options(-Wno-unused-private-field)

    # Clang doesn't add compiler_rt to the search path when compiling with -nostdlib.
    string(REGEX REPLACE "\\.(.*)" "" LLVM_MAJOR_VERSION "${CMAKE_CXX_COMPILER_VERSION}")
    link_directories(${TOOLCHAIN_ROOT}/lib/clang/${LLVM_MAJOR_VERSION}/lib/${SERENITY_ARCH}-pc-serenity/)
endif()

