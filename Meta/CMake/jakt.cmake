set(arch ${CMAKE_SYSTEM_PROCESSOR})
if (arch STREQUAL "x86")
    set(arch "i686")
endif()

if (${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    set(vendor "apple")
else()
    set(vendor "pc")
endif()

if (${CMAKE_SYSTEM_NAME} STREQUAL "Windows")
    set(os "win32")
elseif (${CMAKE_SYSTEM_NAME} STREQUAL "SerenityOS")
    set(os "serenity")
else()
    string(TOLOWER ${CMAKE_SYSTEM_NAME} os)
endif()

set(JAKT_COMPILER "${SerenityOS_SOURCE_DIR}/Toolchain/Local/jakt/bin/jakt")
set(JAKT_TARGET_TRIPLE ${arch}-${vendor}-${os}-unknown)
if(${PROJECT_NAME} STREQUAL "Lagom")
    set(JAKT_TARGET_TRIPLE ${arch}-unknown-${os}-unknown)
    set(JAKT_LIBRARY_BASE "${SerenityOS_SOURCE_DIR}/Toolchain/Local/jakt/lib")
else()
    set(JAKT_LIBRARY_BASE "${SerenityOS_SOURCE_DIR}/Toolchain/Local/jakt/usr/local/lib")
endif()
set(JAKT_LIBRARY_DIR "${JAKT_LIBRARY_BASE}/${JAKT_TARGET_TRIPLE}")
set(JAKT_INCLUDE_DIR "${SerenityOS_SOURCE_DIR}/Toolchain/Local/jakt/usr/local/include/runtime")

# Make sure the jakt compiler:
# - exists
# - can do C++ interop
# Otherwise, complain and just disable jakt for the project.
if (ENABLE_JAKT)
    if (NOT EXISTS ${JAKT_COMPILER})
        message(WARNING "Jakt compiler not found at ${JAKT_COMPILER}, disabling jakt")
        set(ENABLE_JAKT OFF CACHE BOOL "Enable jakt" FORCE)
    else()
        file(WRITE ${CMAKE_BINARY_DIR}/jakt_test.jakt "import extern \"LibMain/Main.h\"\nfn main() { }\n")
        set(include_args)
        set(all_includes
            ${INCLUDE_DIRECTORIES}
            ${SerenityOS_SOURCE_DIR}
            ${SerenityOS_SOURCE_DIR}/Userland/Libraries
            ${SerenityOS_SOURCE_DIR}/Userland/Libraries/LibCrypt
            ${SerenityOS_SOURCE_DIR}/Userland/Libraries/LibSystem
            ${SerenityOS_SOURCE_DIR}/Userland/Services
            ${SerenityOS_SOURCE_DIR}/Userland
        )
        foreach(include IN LISTS all_includes)
            list(APPEND include_args "-I" "${include}")
        endforeach()

        execute_process(
            COMMAND ${JAKT_COMPILER}
                -S
                --ak-is-my-only-stdlib
                -T ${JAKT_TARGET_TRIPLE}
                --binary-dir ${CMAKE_BINARY_DIR}
                ${include_args}
                ${CMAKE_BINARY_DIR}/jakt_test.jakt
            RESULT_VARIABLE JAKT_COMPILER_RESULT
            OUTPUT_QUIET
            ERROR_QUIET
        )

        if (NOT JAKT_COMPILER_RESULT EQUAL 0)
            message(WARNING "Jakt compiler at ${JAKT_COMPILER} does not support C++ interop, disabling jakt")
            set(ENABLE_JAKT OFF CACHE BOOL "Enable jakt" FORCE)
        endif()
    endif()
endif()

if (NOT ENABLE_JAKT)
    return()
endif()

cmake_host_system_information(RESULT JAKT_PROCESSOR_COUNT QUERY NUMBER_OF_PHYSICAL_CORES)
set_property(GLOBAL PROPERTY JOB_POOLS jakt_pool=1)

set(JAKT_COMPILER ${JAKT_COMPILER} CACHE PATH "Path to the jakt compiler" FORCE)
set(JAKT_TARGET_TRIPLE ${JAKT_TARGET_TRIPLE} CACHE STRING "Target triple for the jakt compiler" FORCE)
set(JAKT_LIBRARY_DIR ${JAKT_LIBRARY_DIR} CACHE PATH "Path to the jakt library directory" FORCE)
set(JAKT_INCLUDE_DIR ${JAKT_INCLUDE_DIR} CACHE PATH "Path to the jakt include directory" FORCE)
set(JAKT_PROCESSOR_COUNT ${JAKT_PROCESSOR_COUNT} CACHE STRING "Number of physical cores" FORCE)

message(STATUS "Using jakt compiler at ${JAKT_COMPILER}")
message(STATUS "Using jakt target triple ${JAKT_TARGET_TRIPLE}")
message(STATUS "Using jakt library directory ${JAKT_LIBRARY_DIR}")

function(add_jakt_executable target source)
    cmake_parse_arguments(PARSE_ARGV 2 JAKT_EXECUTABLE "" "" "INCLUDES;CONFIGS;LINK_LIBRARIES;EXTRA_CPP_SOURCES")
    set(configs)
    set(includes)
    set(extra_cpp_sources)

    foreach(config IN LISTS JAKT_EXECUTABLE_CONFIGS)
        list(APPEND configs "--config" "${config}")
    endforeach()
    foreach(include IN LISTS JAKT_EXECUTABLE_INCLUDES)
        list(APPEND includes "-I" "${include}")
    endforeach()
    foreach(source IN LISTS JAKT_EXECUTABLE_EXTRA_CPP_SOURCES)
        get_filename_component(source ${source} ABSOLUTE)
        list(APPEND extra_cpp_sources "-X" "${source}")
    endforeach()

    set(JAKT_EXECUTABLE_NAME ${target})
    set(EXECUTABLE_BINARY_DIR "${CMAKE_CURRENT_BINARY_DIR}/CMakeFiles/${JAKT_EXECUTABLE_NAME}.dir")
    set(EMPTY_SOURCE_FILE "${EXECUTABLE_BINARY_DIR}/empty_cmake_hack.cpp")
    set(main_output_name "${JAKT_EXECUTABLE_NAME}.a")
    set(main_output "${EXECUTABLE_BINARY_DIR}/build/${main_output_name}")
    set(depfile "${EXECUTABLE_BINARY_DIR}/build/${main_output_name}.M")

    cmake_path(ABSOLUTE_PATH source
        BASE_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        OUTPUT_VARIABLE source_abs
    )

    add_executable(${JAKT_EXECUTABLE_NAME} ${EMPTY_SOURCE_FILE})
    get_property(compile_flags TARGET ${JAKT_EXECUTABLE_NAME} PROPERTY COMPILE_OPTIONS)

    set(extra_cpp_flags "--extra-cpp-flag-std=c++2b") # FIXME: CMake should be setting this, but sometimes (e.g. macOS /usr/bin/c++ for lagom) it doesn't;
                                                      #        Passing it here allows us to build on AppleClang 15, and if CMake starts setting it, it will be overridden by later flags.
    foreach(flag IN LISTS compile_flags)
        list(APPEND extra_cpp_flags "--extra-cpp-flag${flag}")
    endforeach()

    set(sysroot_arg)
    if (CMAKE_SYSROOT)
        set(sysroot_arg "--extra-cpp-flag--sysroot=${CMAKE_SYSROOT}")
    endif()

    add_custom_command(
        OUTPUT ${main_output}
        COMMAND ${JAKT_COMPILER}
            -d -O -J ${JAKT_PROCESSOR_COUNT}
            --ak-is-my-only-stdlib
            -T ${JAKT_TARGET_TRIPLE}
            --binary-dir "${EXECUTABLE_BINARY_DIR}/build"
            --exports-dir "${CMAKE_CURRENT_BINARY_DIR}"
            -C ${CMAKE_CXX_COMPILER}
            --archiver ${CMAKE_AR}
            --link-archive ${main_output_name}
            --runtime-library-path ${JAKT_LIBRARY_BASE}
            --dep-file ${depfile}
            ${sysroot_arg}
            ${extra_cpp_flags}
            --extra-cpp-flag-Wno-unused-parameter
            --extra-cpp-flag-Wno-unused-variable
            --extra-cpp-flag-Wno-unused-value
            ${configs}
            ${includes}
            ${extra_cpp_sources}
            "${source_abs}"
        DEPFILE ${depfile}
        DEPENDS
            ${JAKT_EXECUTABLE_EXTRA_CPP_SOURCES}
        COMMENT "Jakt: Compile ${source}"
        JOB_POOL jakt_pool
    )
    add_custom_target(${JAKT_EXECUTABLE_NAME}.t
        DEPENDS ${main_output}
    )

    add_custom_command(
        OUTPUT ${EMPTY_SOURCE_FILE}
        COMMAND ${CMAKE_COMMAND} -E touch ${EMPTY_SOURCE_FILE}
    )

    add_dependencies(${JAKT_EXECUTABLE_NAME} ${JAKT_EXECUTABLE_NAME}.t)

    add_library(${JAKT_EXECUTABLE_NAME}.lib STATIC IMPORTED)
    set_property(TARGET ${JAKT_EXECUTABLE_NAME}.lib PROPERTY IMPORTED_LOCATION ${main_output})

    target_link_libraries(${JAKT_EXECUTABLE_NAME}
        PRIVATE
        ${JAKT_LIBRARY_DIR}/libjakt_main_${JAKT_TARGET_TRIPLE}.a
        ${JAKT_EXECUTABLE_NAME}.lib
        ${JAKT_LIBRARY_DIR}/libjakt_runtime_${JAKT_TARGET_TRIPLE}.a
        ${JAKT_EXECUTABLE_LINK_LIBRARIES}
    )
endfunction()

function(serenity_jakt_app target_name source)
    cmake_parse_arguments(PARSE_ARGV 2 SERENITY_JAKT_EXECUTABLE "" "" "INCLUDES;CONFIGS;LINK_LIBRARIES")
    add_jakt_executable(${target_name} ${source}
        INCLUDES
            ${INCLUDE_DIRECTORIES}
            ${PROJECT_SOURCE_DIR}
            ${PROJECT_SOURCE_DIR}/Userland/Libraries
            ${PROJECT_SOURCE_DIR}/Userland/Libraries/LibCrypt
            ${PROJECT_SOURCE_DIR}/Userland/Libraries/LibSystem
            ${PROJECT_SOURCE_DIR}/Userland/Services
            ${PROJECT_SOURCE_DIR}/Userland
            ${PROJECT_BINARY_DIR}
            ${PROJECT_BINARY_DIR}/Userland/Services
            ${PROJECT_BINARY_DIR}/Userland/Libraries
            ${PROJECT_BINARY_DIR}/Userland
            ${SERENITY_JAKT_EXECUTABLE_INCLUDES}
        CONFIGS ${SERENITY_JAKT_EXECUTABLE_CONFIGS}
        LINK_LIBRARIES ${SERENITY_JAKT_EXECUTABLE_LINK_LIBRARIES}
    )
    set_target_properties(${target_name} PROPERTIES EXCLUDE_FROM_ALL TRUE)
    install(TARGETS ${target_name} RUNTIME DESTINATION bin OPTIONAL)
    serenity_generated_sources(${target_name})
    serenity_set_implicit_links(${target_name})
endfunction(serenity_jakt_app)

function(serenity_jakt_executable target_name)
    cmake_parse_arguments(PARSE_ARGV 1 SERENITY_JAKT_EXECUTABLE "" "" "MAIN_SOURCE;CPP_SOURCES;CONFIGS;LINK_LIBRARIES")
    add_jakt_executable(${target_name} ${SERENITY_JAKT_EXECUTABLE_MAIN_SOURCE}
        INCLUDES
            ${INCLUDE_DIRECTORIES}
            ${PROJECT_SOURCE_DIR}
            ${PROJECT_SOURCE_DIR}/Userland/Libraries
            ${PROJECT_SOURCE_DIR}/Userland/Libraries/LibCrypt
            ${PROJECT_SOURCE_DIR}/Userland/Libraries/LibSystem
            ${PROJECT_SOURCE_DIR}/Userland/Services
            ${PROJECT_SOURCE_DIR}/Userland
            ${PROJECT_BINARY_DIR}
            ${PROJECT_BINARY_DIR}/Userland/Services
            ${PROJECT_BINARY_DIR}/Userland/Libraries
            ${PROJECT_BINARY_DIR}/Userland
            ${SERENITY_JAKT_EXECUTABLE_INCLUDES}
        CONFIGS ${SERENITY_JAKT_EXECUTABLE_CONFIGS}
        LINK_LIBRARIES ${SERENITY_JAKT_EXECUTABLE_LINK_LIBRARIES}
        EXTRA_CPP_SOURCES ${SERENITY_JAKT_EXECUTABLE_CPP_SOURCES}
    )
endfunction()
