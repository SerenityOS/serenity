set(SOURCES
    syscall.cpp
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib")
serenity_libc(LibSystem system)
target_include_directories(LibSystem PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
