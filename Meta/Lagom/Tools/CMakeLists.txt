function(lagom_tool tool)
    cmake_parse_arguments(LAGOM_TOOL "" "" "SOURCES" ${ARGN})
    add_executable(${tool} ${SOURCES} ${LAGOM_TOOL_SOURCES})
    # alias for parity with exports
    add_executable(Lagom::${tool} ALIAS ${tool})
    target_link_libraries(${tool} LagomCore)
    install(
        TARGETS ${tool}
        EXPORT LagomTargets
        RUNTIME COMPONENT Lagom_Runtime
    )
endfunction()

add_subdirectory(ConfigureComponents)
add_subdirectory(CodeGenerators)
