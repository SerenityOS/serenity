set(SOURCES
    Wrappers.cpp
    syscall.cpp
)

# FIXME: This is a hack to avoid a circular dependency with LibC. Figure out a better way.
if ("${SERENITY_ARCH}" STREQUAL "i686")
    set_source_files_properties(${SOURCES} PROPERTIES COMPILE_FLAGS "-fno-stack-protector")
endif()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib")
serenity_libc(LibSystem system)
target_include_directories(LibSystem PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

add_library(LibSystemStatic STATIC ${SOURCES})
target_include_directories(LibSystemStatic PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
