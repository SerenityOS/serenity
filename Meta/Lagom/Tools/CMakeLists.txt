function(lagom_tool tool)
    cmake_parse_arguments(LAGOM_TOOL "" "" "SOURCES;LIBS" ${ARGN})
    add_executable(${tool} ${SOURCES} ${LAGOM_TOOL_SOURCES})
    # alias for parity with exports
    add_executable(Lagom::${tool} ALIAS ${tool})
    target_link_libraries(${tool} AK LibCoreMinimal LibFileSystem GenericClangPlugin ${LAGOM_TOOL_LIBS})
    install(
        TARGETS ${tool}
        EXPORT LagomTargets
        RUNTIME COMPONENT Lagom_Runtime
    )
endfunction()

add_subdirectory(CodeGenerators)
add_subdirectory(ConfigureComponents)
add_subdirectory(PrekernelPEImageGenerator)
add_subdirectory(IPCMagicLinter)

include(jakt)

if (APPLE)
    # FIXME: Remove this once https://openradar.appspot.com/21478051 is fixed.
    find_package(Python3 REQUIRED COMPONENTS Interpreter)
    add_custom_command(
        OUTPUT "${CMAKE_BINARY_DIR}/patched_mac_headers/stamp"
        COMMAND "${Python3_EXECUTABLE}" "${SerenityOS_SOURCE_DIR}/Meta/Lagom/Tools/patch_mac_sdk_headers_for_cxx26.py" -o "${CMAKE_BINARY_DIR}/patched_mac_headers" --sdk-path "${CMAKE_OSX_SYSROOT}"
        COMMAND "${CMAKE_COMMAND}" -E touch "${CMAKE_BINARY_DIR}/patched_mac_headers/stamp"
        VERBATIM
        DEPENDS "${SerenityOS_SOURCE_DIR}/Meta/Lagom/Tools/patch_mac_sdk_headers_for_cxx26.py"
    )
    add_custom_target(patched_mac_headers DEPENDS "${CMAKE_BINARY_DIR}/patched_mac_headers/stamp")
endif()
