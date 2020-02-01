set(SOURCES
    Generate_CSS_PropertyID_cpp.cpp
)

set(CMAKE_CXX_COMPILER g++)
unset(CMAKE_SYSROOT)
set(CMAKE_CXX_FLAGS "-Os -Wall -Wextra -Werror -std=c++17")

add_executable(Generate_CSS_PropertyID_cpp ${SOURCES})
add_dependencies(Generate_CSS_PropertyID_cpp lagom)
target_link_libraries(Generate_CSS_PropertyID_cpp lagom)
