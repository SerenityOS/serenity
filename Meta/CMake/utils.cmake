
include(${CMAKE_CURRENT_LIST_DIR}/serenity_components.cmake)
include(${CMAKE_CURRENT_LIST_DIR}/code_generators.cmake)

function(serenity_install_headers target_name)
    file(GLOB_RECURSE headers RELATIVE ${CMAKE_CURRENT_SOURCE_DIR} "*.h")
    foreach(header ${headers})
        get_filename_component(subdirectory ${header} DIRECTORY)
        install(FILES ${header} DESTINATION usr/include/${target_name}/${subdirectory} OPTIONAL)
    endforeach()
endfunction()

function(serenity_install_sources)
    string(LENGTH ${SerenityOS_SOURCE_DIR} root_source_dir_length)
    string(SUBSTRING ${CMAKE_CURRENT_SOURCE_DIR} ${root_source_dir_length} -1 current_source_dir_relative)
    file(GLOB_RECURSE sources RELATIVE ${CMAKE_CURRENT_SOURCE_DIR} "*.h" "*.cpp")
    foreach(source ${sources})
        get_filename_component(subdirectory ${source} DIRECTORY)
        install(FILES ${source} DESTINATION usr/src/serenity/${current_source_dir_relative}/${subdirectory} OPTIONAL)
    endforeach()
endfunction()

function(serenity_generated_sources target_name)
    if(DEFINED GENERATED_SOURCES)
        set_source_files_properties(${GENERATED_SOURCES} PROPERTIES GENERATED 1)
        foreach(generated ${GENERATED_SOURCES})
            get_filename_component(generated_name ${generated} NAME)
            add_dependencies(${target_name} generate_${generated_name})
            add_dependencies(all_generated generate_${generated_name})
        endforeach()
    endif()
endfunction()

function(serenity_lib target_name fs_name)
    serenity_install_headers(${target_name})
    serenity_install_sources()
    add_library(${target_name} SHARED ${SOURCES} ${GENERATED_SOURCES})
    set_target_properties(${target_name} PROPERTIES EXCLUDE_FROM_ALL TRUE)
    set_target_properties(${target_name} PROPERTIES VERSION "serenity")
    install(TARGETS ${target_name} DESTINATION usr/lib OPTIONAL)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_libc target_name fs_name)
    serenity_install_headers("")
    serenity_install_sources()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -fpic")
    add_library(${target_name} SHARED ${SOURCES})
    install(TARGETS ${target_name} DESTINATION usr/lib)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    # Avoid creating a dependency cycle between system libraries and the C++ standard library. This is necessary
    # to ensure that initialization functions will be called in the right order (libc++ must come after LibPthread).
    if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
        target_link_options(${target_name} PRIVATE -static-libstdc++)
    elseif (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
        target_link_libraries(${target_name} clang_rt.builtins)
        # FIXME: Implement -static-libstdc++ in the next toolchain update.
        target_link_options(${target_name} PRIVATE -nostdlib++ -Wl,-Bstatic -lc++ -Wl,-Bdynamic)
        if (NOT ENABLE_MOLD_LINKER)
            target_link_options(${target_name} PRIVATE -Wl,--no-dependent-libraries)
        endif()
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
    serenity_install_sources()
    add_executable(${target_name} ${SOURCES})
    set_target_properties(${target_name} PROPERTIES EXCLUDE_FROM_ALL TRUE)
    install(TARGETS ${target_name} RUNTIME DESTINATION bin OPTIONAL)
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_test test_src sub_dir)
    cmake_parse_arguments(PARSE_ARGV 2 SERENITY_TEST "MAIN_ALREADY_DEFINED" "CUSTOM_MAIN" "LIBS")
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
    cmake_parse_arguments(PARSE_ARGV 2 SERENITY_TEST "" "CUSTOM_MAIN" "LIBS")
    if ("${SERENITY_TEST_CUSTOM_MAIN}" STREQUAL "")
        set(SERENITY_TEST_CUSTOM_MAIN "$<TARGET_OBJECTS:JavaScriptTestRunnerMain>")
    endif()
    list(APPEND SERENITY_TEST_LIBS LibJS LibCore)
    serenity_test(${test_src} ${sub_dir}
        CUSTOM_MAIN "${SERENITY_TEST_CUSTOM_MAIN}"
        LIBS ${SERENITY_TEST_LIBS})
endfunction()

function(serenity_app target_name)
    cmake_parse_arguments(PARSE_ARGV 1 SERENITY_APP "" "ICON" "")

    serenity_bin("${target_name}")
    set(small_icon "${SerenityOS_SOURCE_DIR}/Base/res/icons/16x16/${SERENITY_APP_ICON}.png")
    set(medium_icon "${SerenityOS_SOURCE_DIR}/Base/res/icons/32x32/${SERENITY_APP_ICON}.png")

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

function(embed_resource target section file)
    get_filename_component(asm_file "${file}" NAME)
    set(asm_file "${CMAKE_CURRENT_BINARY_DIR}/${target}-${section}.s")
    get_filename_component(input_file "${file}" ABSOLUTE)
    file(SIZE "${input_file}" file_size)
    add_custom_command(
        OUTPUT "${asm_file}"
        COMMAND "${SerenityOS_SOURCE_DIR}/Meta/generate-embedded-resource-assembly.sh" "${asm_file}" "${section}" "${input_file}" "${file_size}"
        DEPENDS "${input_file}" "${SerenityOS_SOURCE_DIR}/Meta/generate-embedded-resource-assembly.sh"
        COMMENT "Generating ${asm_file}"
    )
    target_sources("${target}" PRIVATE "${asm_file}")
endfunction()

function(link_with_unicode_data target)
    if (ENABLE_UNICODE_DATABASE_DOWNLOAD)
        target_link_libraries("${target}" LibUnicodeData)
    endif()
endfunction()

function(remove_path_if_version_changed version version_file cache_path)
    set(version_differs YES)

    if (EXISTS "${version_file}")
        file(STRINGS "${version_file}" active_version)
        if (version STREQUAL active_version)
            set(version_differs NO)
        endif()
    endif()

    if (version_differs)
        message(STATUS "Removing outdated ${cache_path} for version ${version}")
        file(REMOVE_RECURSE "${cache_path}")
        file(WRITE "${version_file}" "${version}")
    endif()
endfunction()

function(invoke_generator name generator version_file prefix header implementation)
    cmake_parse_arguments(invoke_generator "" "" "arguments" ${ARGN})

    add_custom_command(
        OUTPUT "${header}" "${implementation}"
        COMMAND $<TARGET_FILE:${generator}> -h "${header}.tmp" -c "${implementation}.tmp" ${invoke_generator_arguments}
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different "${header}.tmp" "${header}"
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different "${implementation}.tmp" "${implementation}"
        COMMAND "${CMAKE_COMMAND}" -E remove "${header}.tmp" "${implementation}.tmp"
        VERBATIM
        DEPENDS ${generator} "${version_file}"
    )

    add_custom_target("generate_${prefix}${name}" DEPENDS "${header}" "${implementation}")
    add_dependencies(all_generated "generate_${prefix}${name}")
endfunction()

function(download_file url path)
    if (NOT EXISTS "${path}")
        get_filename_component(file "${path}" NAME)
        message(STATUS "Downloading file ${file} from ${url}")

        file(DOWNLOAD "${url}" "${path}" INACTIVITY_TIMEOUT 10 STATUS download_result)
        list(GET download_result 0 status_code)
        list(GET download_result 1 error_message)

        if (NOT status_code EQUAL 0)
            file(REMOVE "${path}")
            message(FATAL_ERROR "Failed to download ${url}: ${error_message}")
        endif()
    endif()
endfunction()
