function(serenity_install_headers target_name)
    file(GLOB_RECURSE headers RELATIVE ${CMAKE_CURRENT_SOURCE_DIR} "*.h")
    foreach(header ${headers})
        get_filename_component(subdirectory ${header} DIRECTORY)
        install(FILES ${header} DESTINATION usr/include/${target_name}/${subdirectory} OPTIONAL)
    endforeach()
endfunction()

function(serenity_install_sources target_name)
    file(GLOB_RECURSE sources RELATIVE ${CMAKE_CURRENT_SOURCE_DIR} "*.h" "*.cpp")
    foreach(source ${sources})
        get_filename_component(subdirectory ${source} DIRECTORY)
        install(FILES ${source} DESTINATION usr/src/serenity/${target_name}/${subdirectory} OPTIONAL)
    endforeach()
endfunction()

function(serenity_generated_sources target_name)
    if(DEFINED GENERATED_SOURCES)
        set_source_files_properties(${GENERATED_SOURCES} PROPERTIES GENERATED 1)
        foreach(generated ${GENERATED_SOURCES})
            get_filename_component(generated_name ${generated} NAME)
            add_dependencies(${target_name} generate_${generated_name})
        endforeach()
    endif()
endfunction()

function(serenity_lib target_name fs_name)
    serenity_install_headers(${target_name})
    serenity_install_sources("Userland/Libraries/${target_name}")
    add_library(${target_name} SHARED ${SOURCES} ${GENERATED_SOURCES})
    set_target_properties(${target_name} PROPERTIES EXCLUDE_FROM_ALL TRUE)
    install(TARGETS ${target_name} DESTINATION usr/lib OPTIONAL)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_shared_lib target_name fs_name)
    serenity_install_headers(${target_name})
    serenity_install_sources("Userland/Libraries/${target_name}")
    add_library(${target_name} SHARED ${SOURCES} ${GENERATED_SOURCES})
    set_target_properties(${target_name} PROPERTIES EXCLUDE_FROM_ALL TRUE)
    install(TARGETS ${target_name} DESTINATION usr/lib OPTIONAL)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_libc target_name fs_name)
    serenity_install_headers("")
    serenity_install_sources("Userland/Libraries/LibC")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -fpic")
    add_library(${target_name} SHARED ${SOURCES})
    install(TARGETS ${target_name} DESTINATION usr/lib)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    if (USE_CLANG_TOOLCHAIN)
        target_link_libraries(${target_name} clang_rt.builtins-${SERENITY_CLANG_ARCH})
    endif()
    target_link_directories(LibC PUBLIC ${CMAKE_CURRENT_BINARY_DIR})
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_libc_static target_name fs_name)
    serenity_install_headers("")
    serenity_install_sources("Userland/Libraries/LibC")
    add_library(${target_name} ${SOURCES})
    set_target_properties(${target_name} PROPERTIES EXCLUDE_FROM_ALL TRUE)
    install(TARGETS ${target_name} ARCHIVE DESTINATION usr/lib OPTIONAL)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    target_link_directories(${target_name} PUBLIC ${CMAKE_CURRENT_BINARY_DIR})
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_bin target_name)
    add_executable(${target_name} ${SOURCES})
    set_target_properties(${target_name} PROPERTIES EXCLUDE_FROM_ALL TRUE)
    install(TARGETS ${target_name} RUNTIME DESTINATION bin OPTIONAL)
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_test test_src sub_dir)
    cmake_parse_arguments(SERENITY_TEST "MAIN_ALREADY_DEFINED" "CUSTOM_MAIN" "LIBS" ${ARGN})
    set(TEST_SOURCES ${test_src})
    if ("${SERENITY_TEST_CUSTOM_MAIN}" STREQUAL "")
        set(SERENITY_TEST_CUSTOM_MAIN "$<TARGET_OBJECTS:LibTestMain>")
    endif()
    if (NOT ${SERENITY_TEST_MAIN_ALREADY_DEFINED})
        list(PREPEND TEST_SOURCES "${SERENITY_TEST_CUSTOM_MAIN}")
    endif()
    get_filename_component(test_name ${test_src} NAME_WE)
    add_executable(${test_name} ${TEST_SOURCES})
    add_dependencies(ComponentTests ${test_name})
    set_target_properties(${test_name} PROPERTIES EXCLUDE_FROM_ALL TRUE)
    target_link_libraries(${test_name} LibTest LibCore)
    foreach(lib ${SERENITY_TEST_LIBS})
        target_link_libraries(${test_name} ${lib})
    endforeach()
    install(TARGETS ${test_name} RUNTIME DESTINATION usr/Tests/${sub_dir} OPTIONAL)
endfunction()


function(serenity_testjs_test test_src sub_dir)
    cmake_parse_arguments(SERENITY_TEST "" "CUSTOM_MAIN" "LIBS" ${ARGN})
    if ("${SERENITY_TEST_CUSTOM_MAIN}" STREQUAL "")
        set(SERENITY_TEST_CUSTOM_MAIN "$<TARGET_OBJECTS:JavaScriptTestRunnerMain>")
    endif()
    list(APPEND SERENITY_TEST_LIBS LibJS LibCore)
    serenity_test(${test_src} ${sub_dir}
        CUSTOM_MAIN "${SERENITY_TEST_CUSTOM_MAIN}"
        LIBS ${SERENITY_TEST_LIBS})
endfunction()

function(serenity_app target_name)
    cmake_parse_arguments(SERENITY_APP "" "ICON" "" ${ARGN})

    serenity_bin("${target_name}")
    set(small_icon "${CMAKE_SOURCE_DIR}/Base/res/icons/16x16/${SERENITY_APP_ICON}.png")
    set(medium_icon "${CMAKE_SOURCE_DIR}/Base/res/icons/32x32/${SERENITY_APP_ICON}.png")

    if (EXISTS "${small_icon}")
        embed_resource("${target_name}" serenity_icon_s "${small_icon}")
    else()
        message(FATAL_ERROR "Missing small app icon: ${small_icon}")
    endif()

    if (EXISTS "${medium_icon}")
        embed_resource("${target_name}" serenity_icon_m "${medium_icon}")
    else()
        # These icons are designed small only for use in applets, and thus are exempt.
        list(APPEND allowed_missing_medium_icons "audio-volume-high")
        list(APPEND allowed_missing_medium_icons "edit-copy")

        if (NOT ${SERENITY_APP_ICON} IN_LIST allowed_missing_medium_icons)
            message(FATAL_ERROR "Missing medium app icon: ${medium_icon}")
        endif()
    endif()
endfunction()

define_property(TARGET PROPERTY SERENITY_COMPONENT_NAME BRIEF_DOCS "SerenityOS component name" FULL_DOCS "-")
define_property(TARGET PROPERTY SERENITY_COMPONENT_DESCRIPTION BRIEF_DOCS "SerenityOS component description" FULL_DOCS "-")
define_property(TARGET PROPERTY SERENITY_COMPONENT_RECOMMENDED BRIEF_DOCS "SerenityOS component recommended (flag)" FULL_DOCS "-")
define_property(TARGET PROPERTY SERENITY_COMPONENT_REQUIRED BRIEF_DOCS "SerenityOS component required (flag)" FULL_DOCS "-")
define_property(TARGET PROPERTY SERENITY_COMPONENT_DEPENDS BRIEF_DOCS "SerenityOS component dependencies" FULL_DOCS "-")

function(serenity_component name)
    cmake_parse_arguments(SERENITY_COMPONENT "RECOMMENDED;REQUIRED" "DESCRIPTION" "TARGETS;DEPENDS" ${ARGN})
    string(TOUPPER "${name}" NAME_UPPER)
    option("BUILD_${NAME_UPPER}" "Build ${name}" ${SERENITY_COMPONENT_RECOMMENDED})
    add_custom_target("Component${name}")
    set_property(TARGET "Component${name}" PROPERTY SERENITY_COMPONENT_NAME ${name})
    set_property(TARGET "Component${name}" PROPERTY SERENITY_COMPONENT_DESCRIPTION ${SERENITY_COMPONENT_DESCRIPTION})
    set_property(TARGET "Component${name}" PROPERTY SERENITY_COMPONENT_RECOMMENDED ${SERENITY_COMPONENT_RECOMMENDED})
    set_property(TARGET "Component${name}" PROPERTY SERENITY_COMPONENT_REQUIRED ${SERENITY_COMPONENT_REQUIRED})
    set_property(TARGET "Component${name}" PROPERTY SERENITY_COMPONENT_DEPENDS ${SERENITY_COMPONENT_DEPENDS})
    if(NOT "${SERENITY_COMPONENT_TARGETS}" STREQUAL "")
        foreach(target IN LISTS SERENITY_COMPONENT_TARGETS)
          add_dependencies("Component${name}" "${target}")
        endforeach()
    endif()
    if(BUILD_EVERYTHING OR BUILD_${NAME_UPPER} OR SERENITY_COMPONENT_REQUIRED)
        add_dependencies(components "Component${name}")
    endif()
    foreach(dependency ${SERENITY_COMPONENT_DEPENDS})
        add_dependencies("Component${name}" "Component${dependency}")
    endforeach()
endfunction()

macro(export_components_helper file_name current_dir)
    get_property(sub_dirs DIRECTORY ${current_dir} PROPERTY SUBDIRECTORIES)
    foreach(sub_dir ${sub_dirs})
        export_components_helper(${file_name} ${sub_dir})
    endforeach()

    get_property(targets DIRECTORY ${current_dir} PROPERTY BUILDSYSTEM_TARGETS)
    foreach(target ${targets})
        get_property(component_name TARGET ${target} PROPERTY SERENITY_COMPONENT_NAME)
        if(NOT "${component_name}" STREQUAL "")
            get_property(component_name TARGET ${target} PROPERTY SERENITY_COMPONENT_NAME)
            get_property(component_description TARGET ${target} PROPERTY SERENITY_COMPONENT_DESCRIPTION)
            get_property(component_recommended TARGET ${target} PROPERTY SERENITY_COMPONENT_RECOMMENDED)
            get_property(component_required TARGET ${target} PROPERTY SERENITY_COMPONENT_REQUIRED)
            get_property(component_depends TARGET ${target} PROPERTY SERENITY_COMPONENT_DEPENDS)
            file(APPEND ${file_name} "[${component_name}]\n")
            file(APPEND ${file_name} "description=${component_description}\n")
            if(component_recommended)
                file(APPEND ${file_name} "recommended=1\n")
            else()
                file(APPEND ${file_name} "recommended=0\n")
            endif()
            if(component_required)
                file(APPEND ${file_name} "required=1\n")
            else()
                file(APPEND ${file_name} "required=0\n")
            endif()
            string(TOUPPER "${component_name}" component_name_upper)
            if(BUILD_${component_name_upper})
                file(APPEND ${file_name} "user_selected=1\n")
            else()
                file(APPEND ${file_name} "user_selected=0\n")
            endif()

            file(APPEND ${file_name} "depends=${component_depends}\n")
            file(APPEND ${file_name} "\n")
        endif()
    endforeach()
endmacro()

function(export_components file_name)
    file(WRITE ${file_name} "[Global]\n")
    if(BUILD_EVERYTHING)
        file(APPEND ${file_name} "build_everything=1\n")
    else()
        file(APPEND ${file_name} "build_everything=0\n")
    endif()
    file(APPEND ${file_name} "\n")
    export_components_helper(${file_name} ${CMAKE_CURRENT_SOURCE_DIR})
endfunction()

function(compile_gml source output string_name)
    set(source ${CMAKE_CURRENT_SOURCE_DIR}/${source})
    add_custom_command(
        OUTPUT ${output}
        COMMAND ${write_if_different} ${output} ${CMAKE_SOURCE_DIR}/Meta/text-to-cpp-string.sh ${string_name} ${source}
        VERBATIM
        DEPENDS ${CMAKE_SOURCE_DIR}/Meta/text-to-cpp-string.sh
        MAIN_DEPENDENCY ${source}
    )
    get_filename_component(output_name ${output} NAME)
    add_custom_target(generate_${output_name} DEPENDS ${output})
endfunction()


function(compile_ipc source output)
    set(source ${CMAKE_CURRENT_SOURCE_DIR}/${source})
    add_custom_command(
        OUTPUT ${output}
        COMMAND ${write_if_different} ${output} ${CMAKE_BINARY_DIR}/Userland/DevTools/IPCCompiler/IPCCompiler ${source}
        VERBATIM
        DEPENDS IPCCompiler
        MAIN_DEPENDENCY ${source}
    )
    get_filename_component(output_name ${output} NAME)
    add_custom_target(generate_${output_name} DEPENDS ${output})
endfunction()

function(compile_protobuf source header)
    get_filename_component(header_name ${header} NAME)
    set(target_name "generate_${header_name}")
    # Note: This function is called atleast twice with the same header, once in the kernel
    #       and once in Userland by things that want to access the descirbed API
    if(NOT TARGET ${target_name})
        set(source ${CMAKE_CURRENT_SOURCE_DIR}/${source})
        set(output ${CMAKE_CURRENT_BINARY_DIR}/${header})
        add_custom_command(
            OUTPUT ${output}
            COMMAND ${write_if_different} ${output} ${CMAKE_BINARY_DIR}/Userland/DevTools/ProtoBufCompiler/ProtoBufCompiler ${source}
            VERBATIM
            DEPENDS ProtoBufCompiler
            MAIN_DEPENDENCY ${source}
        )
        add_custom_target(${target_name} DEPENDS ${output})
    endif()
endfunction()

function(embed_resource target section file)
    get_filename_component(asm_file "${file}" NAME)
    set(asm_file "${CMAKE_CURRENT_BINARY_DIR}/${target}-${section}.s")
    get_filename_component(input_file "${file}" ABSOLUTE)
    file(SIZE "${input_file}" file_size)
    add_custom_command(
        OUTPUT "${asm_file}"
        COMMAND "${CMAKE_SOURCE_DIR}/Meta/generate-embedded-resource-assembly.sh" "${asm_file}" "${section}" "${input_file}" "${file_size}"
        DEPENDS "${input_file}" "${CMAKE_SOURCE_DIR}/Meta/generate-embedded-resource-assembly.sh"
        COMMENT "Generating ${asm_file}"
    )
    target_sources("${target}" PRIVATE "${asm_file}")
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
            COMMAND ${write_if_different} ${output} ${CMAKE_BINARY_DIR}/Userland/DevTools/StateMachineGenerator/StateMachineGenerator ${source}
            VERBATIM
            DEPENDS StateMachineGenerator
            MAIN_DEPENDENCY ${source}
        )
        add_custom_target(${target_name} DEPENDS ${output})
    endif()
endfunction()
