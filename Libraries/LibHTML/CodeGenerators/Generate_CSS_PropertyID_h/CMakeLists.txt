set(SOURCES
    Generate_CSS_PropertyID_h.cpp
)

set(CMAKE_CXX_COMPILER g++)
unset(CMAKE_SYSROOT)
set(CMAKE_CXX_FLAGS "-Os -Wall -Wextra -Werror -std=c++17")

add_executable(Generate_CSS_PropertyID_h ${SOURCES})
add_dependencies(Generate_CSS_PropertyID_h lagom)
target_link_libraries(Generate_CSS_PropertyID_h lagom)
