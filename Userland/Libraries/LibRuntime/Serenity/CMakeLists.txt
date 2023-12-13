set(LIBRUNTIME_SOURCES
    Mutex.cpp
    System.cpp
)

if (SERENITY_ARCH STREQUAL "x86_64")
    set(LIBRUNTIME_ARCH_SOURCES
        LibC/Arch/x86_64/memset.cpp
        LibC/Arch/x86_64/memset.S
    )
endif()

list(APPEND LIBRUNTIME_SOURCES ${LIBRUNTIME_ARCH_SOURCES})
list(TRANSFORM LIBRUNTIME_SOURCES PREPEND "${CMAKE_CURRENT_SOURCE_DIR}/")
set(LIBRUNTIME_SOURCES ${LIBRUNTIME_SOURCES} PARENT_SCOPE)

add_library(crt0 STATIC LibC/crt0.cpp)
add_dependencies(crt0 install_libc_headers)
target_link_libraries(crt0 PRIVATE NoCoverage)
add_custom_command(
    TARGET crt0
    COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_OBJECTS:crt0> ${CMAKE_INSTALL_PREFIX}/usr/lib/crt0.o
)

add_library(crt0_shared STATIC LibC/crt0_shared.cpp)
add_dependencies(crt0_shared install_libc_headers)
target_link_libraries(crt0_shared PRIVATE NoCoverage)
add_custom_command(
    TARGET crt0_shared
    COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_OBJECTS:crt0_shared> ${CMAKE_INSTALL_PREFIX}/usr/lib/crt0_shared.o
)

add_library(crti STATIC LibC/Arch/${SERENITY_ARCH}/crti.S)
target_link_libraries(crti PRIVATE NoCoverage)
add_custom_command(
    TARGET crti
    COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_OBJECTS:crti> ${CMAKE_INSTALL_PREFIX}/usr/lib/crti.o
)

add_library(crtn STATIC LibC/Arch/${SERENITY_ARCH}/crtn.S)
target_link_libraries(crtn PRIVATE NoCoverage)
add_custom_command(
    TARGET crtn
    COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_OBJECTS:crtn> ${CMAKE_INSTALL_PREFIX}/usr/lib/crtn.o
)
