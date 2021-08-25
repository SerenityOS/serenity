set(SOURCES
    boot.S
    multiboot.S
    init.cpp
    UBSanitizer.cpp
    ../MiniStdLib.cpp
    ../../Userland/Libraries/LibELF/Relocation.cpp
)

if ("${SERENITY_ARCH}" STREQUAL "i686")
    set(PREKERNEL_TARGET Prekernel32)
else()
    set(PREKERNEL_TARGET Prekernel64)
endif()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -static")

add_executable(${PREKERNEL_TARGET} ${SOURCES})
target_compile_options(${PREKERNEL_TARGET} PRIVATE -no-pie -fno-pic)

target_link_options(${PREKERNEL_TARGET} PRIVATE LINKER:-T ${CMAKE_CURRENT_SOURCE_DIR}/linker.ld -nostdlib LINKER:--no-pie)
set_target_properties(${PREKERNEL_TARGET} PROPERTIES LINK_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/linker.ld)

if (USE_CLANG_TOOLCHAIN)
    target_link_libraries(${PREKERNEL_TARGET} clang_rt.builtins-${SERENITY_CLANG_ARCH} c++abi)
else()
    target_link_libraries(${PREKERNEL_TARGET} gcc supc++)
endif()

add_custom_command(
    TARGET ${PREKERNEL_TARGET} POST_BUILD
    COMMAND ${CMAKE_OBJCOPY} -O elf32-i386 ${CMAKE_CURRENT_BINARY_DIR}/${PREKERNEL_TARGET} ${CMAKE_CURRENT_BINARY_DIR}/Prekernel
    BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/Prekernel
)

install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Prekernel" DESTINATION boot)

# Remove options which the Prekernel environment doesn't support.
get_target_property(PREKERNEL_TARGET_OPTIONS ${PREKERNEL_TARGET} COMPILE_OPTIONS)
list(REMOVE_ITEM PREKERNEL_TARGET_OPTIONS "-fsanitize-coverage=trace-pc")
list(REMOVE_ITEM PREKERNEL_TARGET_OPTIONS "-fsanitize=kernel-address")
set_target_properties(${PREKERNEL_TARGET} PROPERTIES COMPILE_OPTIONS "${PREKERNEL_TARGET_OPTIONS}")
