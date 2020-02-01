set(SOURCES
    main.cpp
)

set(CMAKE_CXX_COMPILER g++)
unset(CMAKE_SYSROOT)
set(CMAKE_CXX_FLAGS "-Os -Wall -Wextra -Werror -std=c++17")

add_executable(IPCCompiler ${SOURCES})
add_dependencies(IPCCompiler lagom)
target_link_libraries(IPCCompiler lagom)
