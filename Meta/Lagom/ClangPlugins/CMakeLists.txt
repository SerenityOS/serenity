find_package(Clang 17 CONFIG REQUIRED)

function(clang_plugin target_name)
    cmake_parse_arguments(CLANG_PLUGIN "" "" "SOURCES" ${ARGN})
    add_library(${target_name} ${CLANG_PLUGIN_SOURCES})
    install(TARGETS ${target_name} DESTINATION ${CMAKE_INSTALL_LIBDIR})
    target_include_directories(${target_name} SYSTEM PRIVATE ${CLANG_INCLUDE_DIRS} ${LLVM_INCLUDE_DIRS})
    target_compile_features(${target_name} PRIVATE cxx_std_20)
    target_compile_options(${target_name} PRIVATE -Wall -Wextra -Werror -Wno-unused -fno-rtti)

    add_custom_target(${target_name}Target DEPENDS ${target_name})

    set_property(GLOBAL APPEND PROPERTY CLANG_PLUGINS_ALL_COMPILE_OPTIONS -fplugin=${CMAKE_BINARY_DIR}/${CMAKE_INSTALL_LIBDIR}/lib${target_name}.so)
endfunction()

clang_plugin(LambdaCaptureClangPlugin SOURCES LambdaCapturePluginAction.cpp)
clang_plugin(LibJSGCClangPlugin SOURCES LibJSGCPluginAction.cpp)
