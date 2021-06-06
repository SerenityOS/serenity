set(SOURCES
    boot.S
    multiboot.S
    init.cpp
    UBSanitizer.cpp
    ../MiniStdLib.cpp
)

if ("${SERENITY_ARCH}" STREQUAL "i686")
    set(PREKERNEL_TARGET Prekernel32)
else()
    set(PREKERNEL_TARGET Prekernel64)
endif()

add_executable(${PREKERNEL_TARGET} ${SOURCES})
target_compile_options(${PREKERNEL_TARGET} PRIVATE -no-pie -fno-pic)

target_link_options(${PREKERNEL_TARGET} PRIVATE LINKER:-T ${CMAKE_CURRENT_SOURCE_DIR}/linker.ld -nostdlib)
set_target_properties(${PREKERNEL_TARGET} PROPERTIES LINK_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/linker.ld)

target_link_libraries(${PREKERNEL_TARGET} gcc supc++)

add_custom_command(
    TARGET ${PREKERNEL_TARGET} POST_BUILD
    COMMAND ${TOOLCHAIN_PREFIX}objcopy -O elf32-i386 ${CMAKE_CURRENT_BINARY_DIR}/${PREKERNEL_TARGET} ${CMAKE_CURRENT_BINARY_DIR}/Prekernel
    BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/Prekernel
)

install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Prekernel" DESTINATION boot)
set_source_files_properties(${SOURCES} PROPERTIES COMPILE_FLAGS "-fno-sanitize-coverage=trace-pc")
