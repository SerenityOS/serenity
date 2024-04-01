#
# Functions for generating sources using host tools
#

function(embed_as_string_view name source_file output source_variable_name)
    cmake_parse_arguments(PARSE_ARGV 4 EMBED_STRING_VIEW "" "NAMESPACE" "")
    set(namespace_arg "")
    if (EMBED_STRING_VIEW_NAMESPACE)
        set(namespace_arg "-s ${EMBED_STRING_VIEW_NAMESPACE}")
    endif()
    find_package(Python3 REQUIRED COMPONENTS Interpreter)
    add_custom_command(
        OUTPUT "${output}"
        COMMAND "${Python3_EXECUTABLE}" "${SerenityOS_SOURCE_DIR}/Meta/embed_as_string_view.py" "${source_file}" -o "${output}.tmp" -n "${source_variable_name}" ${namespace_arg}
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different "${output}.tmp" "${output}"
        COMMAND "${CMAKE_COMMAND}" -E remove "${output}.tmp"
        VERBATIM
        DEPENDS "${SerenityOS_SOURCE_DIR}/Meta/embed_as_string_view.py"
        MAIN_DEPENDENCY "${source_file}"
    )

    add_custom_target("generate_${name}" DEPENDS "${output}")
    add_dependencies(all_generated "generate_${name}")
endfunction()

function(stringify_gml source output string_name)
    set(source ${CMAKE_CURRENT_SOURCE_DIR}/${source})
    get_filename_component(output_name ${output} NAME)
    embed_as_string_view(${output_name} ${source} ${output} ${string_name})
endfunction()

function(compile_gml source output)
    set(source ${CMAKE_CURRENT_SOURCE_DIR}/${source})
    add_custom_command(
        OUTPUT ${output}
        COMMAND $<TARGET_FILE:Lagom::GMLCompiler> ${source} > ${output}.tmp
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different ${output}.tmp ${output}
        COMMAND "${CMAKE_COMMAND}" -E remove ${output}.tmp
        VERBATIM
        DEPENDS Lagom::GMLCompiler
        MAIN_DEPENDENCY ${source}
    )
    get_filename_component(output_name ${output} NAME)
    add_custom_target(generate_${output_name} DEPENDS ${output})
    add_dependencies(all_generated generate_${output_name})
endfunction()

function(compile_ipc source output)
    if (NOT IS_ABSOLUTE ${source})
        set(source ${CMAKE_CURRENT_SOURCE_DIR}/${source})
    endif()
    add_custom_command(
        OUTPUT ${output}
        COMMAND $<TARGET_FILE:Lagom::IPCCompiler> ${source} -o ${output}.tmp
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different ${output}.tmp ${output}
        COMMAND "${CMAKE_COMMAND}" -E remove ${output}.tmp
        VERBATIM
        DEPENDS Lagom::IPCCompiler
        MAIN_DEPENDENCY ${source}
    )
    get_filename_component(output_name ${output} NAME)
    add_custom_target(generate_${output_name} DEPENDS ${output})
    add_dependencies(all_generated generate_${output_name})

    # TODO: Use cmake_path() when we upgrade the minimum CMake version to 3.20
    #       https://cmake.org/cmake/help/v3.23/command/cmake_path.html#relative-path
    string(LENGTH ${SerenityOS_SOURCE_DIR} root_source_dir_length)
    string(SUBSTRING ${CMAKE_CURRENT_SOURCE_DIR} ${root_source_dir_length} -1 current_source_dir_relative)
    install(FILES ${CMAKE_CURRENT_BINARY_DIR}/${output} DESTINATION "${CMAKE_INSTALL_INCLUDEDIR}/${current_source_dir_relative}" OPTIONAL)
endfunction()

function(generate_state_machine source header)
    get_filename_component(header_name ${header} NAME)
    set(target_name "generate_${header_name}")
    # Note: This function is called twice with the same header, once in the kernel
    #       and once in Userland/LibVT, this check makes sure that only one target
    #       is generated for that header.
    if(NOT TARGET ${target_name})
        set(source ${CMAKE_CURRENT_SOURCE_DIR}/${source})
        set(output ${CMAKE_CURRENT_BINARY_DIR}/${header})
        add_custom_command(
            OUTPUT ${output}
            COMMAND $<TARGET_FILE:Lagom::StateMachineGenerator> ${source} -o ${output}.tmp
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different ${output}.tmp ${output}
            COMMAND "${CMAKE_COMMAND}" -E remove ${output}.tmp
            VERBATIM
            DEPENDS Lagom::StateMachineGenerator
            MAIN_DEPENDENCY ${source}
        )
        add_custom_target(${target_name} DEPENDS ${output})
        add_dependencies(all_generated ${target_name})
    endif()
endfunction()

function(compile_wayland_protocol source protocol_name)
    if (NOT IS_ABSOLUTE ${source})
        set(source ${CMAKE_CURRENT_SOURCE_DIR}/${source})
    endif()
    set(output_1 ${CMAKE_CURRENT_BINARY_DIR}/${protocol_name}-protocol.cpp)
    set(output_2 ${CMAKE_CURRENT_BINARY_DIR}/${protocol_name}-protocol.h)
    set(output_3 ${CMAKE_CURRENT_BINARY_DIR}/${protocol_name}-private-protocol.h)

    add_custom_command(
        OUTPUT ${output_1} ${output_2} ${output_3}
        COMMAND $<TARGET_FILE:Lagom::WaylandTranspiler> ${source} ${CMAKE_CURRENT_BINARY_DIR}
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different ${output_1}.tmp ${output_1}
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different ${output_2}.tmp ${output_2}
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different ${output_3}.tmp ${output_3}
        COMMAND "${CMAKE_COMMAND}" -E remove ${output_1}.tmp
        COMMAND "${CMAKE_COMMAND}" -E remove ${output_2}.tmp
        COMMAND "${CMAKE_COMMAND}" -E remove ${output_3}.tmp
        VERBATIM
        DEPENDS Lagom::WaylandTranspiler
        MAIN_DEPENDENCY ${source}
    )
    
    add_custom_target(generate_${protocol_name}-protocol DEPENDS ${output_1} ${output_2} ${output_3})
    add_dependencies(all_generated generate_${protocol_name}-protocol)
endfunction()
