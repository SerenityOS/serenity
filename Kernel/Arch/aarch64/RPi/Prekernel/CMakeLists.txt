set(PREKERNEL_SOURCES
    boot.S
    init.cpp
    ../GPIO.cpp
    ../HardwareClocks.cpp
    ../Mailbox.cpp
    ../MMIO.cpp
    ../UART.cpp
    ../../MainIdRegister.cpp
    ../../../../Library/MiniStdLib.cpp
)

set(SOURCES
    ${PREKERNEL_SOURCES}
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -static")

add_executable(Prekernel ${SOURCES})
target_compile_options(Prekernel PRIVATE
    -pie -fpic -fno-threadsafe-statics        # Position-independent code
    -mstrict-align -Wno-cast-align            # aarch64 alignment requirements
    -fno-sanitize=undefined -fno-sanitize=all # both unnecessary and don't work
    -fno-stack-protector                      # doesn't work without MMU
    -O0
)

get_target_property(PREKERNEL_TARGET_OPTIONS Prekernel COMPILE_OPTIONS)
# Make sure no sanitization options remain in the compiler flags.
list(REMOVE_ITEM PREKERNEL_TARGET_OPTIONS "-fsanitize-coverage=trace-pc")
list(REMOVE_ITEM PREKERNEL_TARGET_OPTIONS "-fsanitize=kernel-address")
list(REMOVE_ITEM PREKERNEL_TARGET_OPTIONS "-O2")
list(REMOVE_ITEM PREKERNEL_TARGET_OPTIONS "-fsanitize=undefined")
set_target_properties(Prekernel PROPERTIES COMPILE_OPTIONS "${PREKERNEL_TARGET_OPTIONS}")

target_link_options(Prekernel PRIVATE LINKER:-T ${CMAKE_CURRENT_SOURCE_DIR}/linker.ld -nostdlib LINKER:--pie)
set_target_properties(Prekernel PROPERTIES LINK_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/linker.ld)

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    target_link_libraries(Prekernel PRIVATE gcc)
elseif(CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    target_link_libraries(Prekernel PRIVATE clang_rt.builtins)
endif()

add_custom_command(
    TARGET Prekernel POST_BUILD
    COMMAND ${CMAKE_OBJCOPY} -O binary Prekernel kernel8.img
    BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/kernel8.img
)

install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Prekernel" DESTINATION boot)
