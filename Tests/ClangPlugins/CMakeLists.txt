find_package(Clang 17 CONFIG REQUIRED)
find_package(LLVM 17 CONFIG REQUIRED)
include(AddLLVM)

find_package(Python3 REQUIRED COMPONENTS Interpreter)

get_property(CLANG_PLUGINS_ALL_COMPILE_OPTIONS GLOBAL PROPERTY CLANG_PLUGINS_ALL_COMPILE_OPTIONS)
list(APPEND CLANG_PLUGINS_ALL_COMPILE_OPTIONS -std=c++20 -Wno-user-defined-literals -Wno-literal-range)

get_property(CLANG_PLUGINS_INCLUDE_DIRECTORIES TARGET AK PROPERTY INCLUDE_DIRECTORIES)
list(APPEND CLANG_PLUGINS_INCLUDE_DIRECTORIES ${CMAKE_CXX_IMPLICIT_INCLUDE_DIRECTORIES})

configure_lit_site_cfg(
    ${CMAKE_CURRENT_SOURCE_DIR}/lit.site.cfg.py.in
    ${CMAKE_CURRENT_BINARY_DIR}/lit.site.cfg.py
    MAIN_CONFIG ${CMAKE_CURRENT_SOURCE_DIR}/lit.cfg.py
    PATHS
        LLVM_BINARY_DIR
        LLVM_TOOLS_DIR
        LLVM_LIBS_DIR
        CMAKE_LIBRARY_OUTPUT_DIRECTORY
        CMAKE_CURRENT_SOURCE_DIR
)

add_custom_command(
    OUTPUT venv
    COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/requirements.txt requirements.txt
    COMMAND ${Python3_EXECUTABLE} -m venv venv
    COMMAND ./venv/bin/pip install -r requirements.txt --upgrade
)
add_custom_target(TestClangPluginsDependencies ALL
    DEPENDS venv JSClangPlugin GenericClangPlugin
    SOURCES requirements.txt
)
add_test(
    NAME TestClangPlugins
    COMMAND ${CMAKE_CURRENT_BINARY_DIR}/venv/bin/lit -v .
)
