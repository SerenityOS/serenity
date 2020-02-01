set(SOURCES
    main.cpp
)

set(CMAKE_CXX_COMPILER g++)
unset(CMAKE_SYSROOT)
set(CMAKE_CXX_FLAGS "-Os -Wall -Wextra -Werror -std=c++17")

add_executable(FormCompiler ${SOURCES})
add_dependencies(FormCompiler lagom)
target_link_libraries(FormCompiler lagom)
