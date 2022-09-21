function (generate_css_implementation)

    if (CMAKE_CURRENT_BINARY_DIR MATCHES ".*/LibWeb")
        # Serenity build
        SET(LIBWEB_INPUT_FOLDER "${CMAKE_CURRENT_SOURCE_DIR}")
        SET(LIBWEB_OUTPUT_FOLDER "")
        SET(LIBWEB_META_PREFIX "")
    else()
        # Lagom Build
        SET(LIBWEB_INPUT_FOLDER "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibWeb")
        SET(LIBWEB_OUTPUT_FOLDER "LibWeb/")
        SET(LIBWEB_META_PREFIX "Lagom")
    endif()

    invoke_generator(
        "Enums.cpp"
        Lagom::GenerateCSSEnums
        "${LIBWEB_INPUT_FOLDER}/CSS/Enums.json"
        "${LIBWEB_META_PREFIX}"
        "${LIBWEB_OUTPUT_FOLDER}CSS/Enums.h"
        "${LIBWEB_OUTPUT_FOLDER}CSS/Enums.cpp"
        arguments -j "${LIBWEB_INPUT_FOLDER}/CSS/Enums.json"
    )

    invoke_generator(
        "MediaFeatureID.cpp"
        Lagom::GenerateCSSMediaFeatureID
        "${LIBWEB_INPUT_FOLDER}/CSS/MediaFeatures.json"
        "${LIBWEB_META_PREFIX}"
        "${LIBWEB_OUTPUT_FOLDER}CSS/MediaFeatureID.h"
        "${LIBWEB_OUTPUT_FOLDER}CSS/MediaFeatureID.cpp"
        arguments -j "${LIBWEB_INPUT_FOLDER}/CSS/MediaFeatures.json"
    )

    invoke_generator(
        "PropertyID.cpp"
        Lagom::GenerateCSSPropertyID
        "${LIBWEB_INPUT_FOLDER}/CSS/Properties.json"
        "${LIBWEB_META_PREFIX}"
        "${LIBWEB_OUTPUT_FOLDER}CSS/PropertyID.h"
        "${LIBWEB_OUTPUT_FOLDER}CSS/PropertyID.cpp"
        arguments -j "${LIBWEB_INPUT_FOLDER}/CSS/Properties.json"
    )

    invoke_generator(
        "TransformFunctions.cpp"
        Lagom::GenerateCSSTransformFunctions
        "${LIBWEB_INPUT_FOLDER}/CSS/TransformFunctions.json"
        "${LIBWEB_META_PREFIX}"
        "${LIBWEB_OUTPUT_FOLDER}CSS/TransformFunctions.h"
        "${LIBWEB_OUTPUT_FOLDER}CSS/TransformFunctions.cpp"
        arguments -j "${LIBWEB_INPUT_FOLDER}/CSS/TransformFunctions.json"
    )

    invoke_generator(
        "ValueID.cpp"
        Lagom::GenerateCSSValueID
        "${LIBWEB_INPUT_FOLDER}/CSS/Identifiers.json"
        "${LIBWEB_META_PREFIX}"
        "${LIBWEB_OUTPUT_FOLDER}CSS/ValueID.h"
        "${LIBWEB_OUTPUT_FOLDER}CSS/ValueID.cpp"
        arguments -j "${LIBWEB_INPUT_FOLDER}/CSS/Identifiers.json"
    )

    add_custom_command(
        OUTPUT ${LIBWEB_OUTPUT_FOLDER}CSS/DefaultStyleSheetSource.cpp
        COMMAND "${CMAKE_COMMAND}" -E make_directory ${LIBWEB_OUTPUT_FOLDER}CSS
        COMMAND "${LIBWEB_INPUT_FOLDER}/Scripts/GenerateStyleSheetSource.sh" default_stylesheet_source "${LIBWEB_INPUT_FOLDER}/CSS/Default.css" > ${LIBWEB_OUTPUT_FOLDER}CSS/DefaultStyleSheetSource.cpp.tmp
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different ${LIBWEB_OUTPUT_FOLDER}CSS/DefaultStyleSheetSource.cpp.tmp ${LIBWEB_OUTPUT_FOLDER}CSS/DefaultStyleSheetSource.cpp
        COMMAND "${CMAKE_COMMAND}" -E remove ${LIBWEB_OUTPUT_FOLDER}CSS/DefaultStyleSheetSource.cpp.tmp
        VERBATIM
        DEPENDS "${LIBWEB_INPUT_FOLDER}/Scripts/GenerateStyleSheetSource.sh"
        MAIN_DEPENDENCY ${LIBWEB_INPUT_FOLDER}/CSS/Default.css
    )
    add_custom_target(generate_${LIBWEB_META_PREFIX}DefaultStyleSheetSource.cpp DEPENDS ${LIBWEB_OUTPUT_FOLDER}CSS/DefaultStyleSheetSource.cpp)
    add_dependencies(all_generated generate_${LIBWEB_META_PREFIX}DefaultStyleSheetSource.cpp)

    add_custom_command(
        OUTPUT ${LIBWEB_OUTPUT_FOLDER}CSS/QuirksModeStyleSheetSource.cpp
        COMMAND "${CMAKE_COMMAND}" -E make_directory ${LIBWEB_OUTPUT_FOLDER}CSS
        COMMAND "${LIBWEB_INPUT_FOLDER}/Scripts/GenerateStyleSheetSource.sh" quirks_mode_stylesheet_source "${LIBWEB_INPUT_FOLDER}/CSS/QuirksMode.css" > ${LIBWEB_OUTPUT_FOLDER}CSS/QuirksModeStyleSheetSource.cpp.tmp
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different ${LIBWEB_OUTPUT_FOLDER}CSS/QuirksModeStyleSheetSource.cpp.tmp ${LIBWEB_OUTPUT_FOLDER}CSS/QuirksModeStyleSheetSource.cpp
        COMMAND "${CMAKE_COMMAND}" -E remove ${LIBWEB_OUTPUT_FOLDER}CSS/QuirksModeStyleSheetSource.cpp.tmp
        VERBATIM
        DEPENDS "${LIBWEB_INPUT_FOLDER}/Scripts/GenerateStyleSheetSource.sh"
        MAIN_DEPENDENCY ${LIBWEB_INPUT_FOLDER}/CSS/Default.css
    )
    add_custom_target(generate_${LIBWEB_META_PREFIX}QuirksModeStyleSheetSource.cpp DEPENDS ${LIBWEB_OUTPUT_FOLDER}CSS/QuirksModeStyleSheetSource.cpp)
    add_dependencies(all_generated generate_${LIBWEB_META_PREFIX}QuirksModeStyleSheetSource.cpp)

endfunction()

function (generate_js_bindings target)

    if (CMAKE_CURRENT_BINARY_DIR MATCHES ".*/LibWeb")
        # Serenity build
        SET(LIBWEB_INPUT_FOLDER "${CMAKE_CURRENT_SOURCE_DIR}")
        SET(LIBWEB_OUTPUT_FOLDER "")
        SET(LIBWEB_META_PREFIX "")
    else()
        # Lagom Build
        SET(LIBWEB_INPUT_FOLDER "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibWeb")
        SET(LIBWEB_OUTPUT_FOLDER "LibWeb/")
        SET(LIBWEB_META_PREFIX "Lagom")
    endif()

    function(libweb_js_bindings class)
        cmake_parse_arguments(PARSE_ARGV 1 LIBWEB_BINDINGS "ITERABLE" "" "")
        get_filename_component(basename "${class}" NAME)
        set(BINDINGS_SOURCES
            "${LIBWEB_OUTPUT_FOLDER}Bindings/${basename}Constructor.h"
            "${LIBWEB_OUTPUT_FOLDER}Bindings/${basename}Constructor.cpp"
            "${LIBWEB_OUTPUT_FOLDER}Bindings/${basename}Prototype.h"
            "${LIBWEB_OUTPUT_FOLDER}Bindings/${basename}Prototype.cpp"
        )
        set(BINDINGS_TYPES
            constructor-header
            constructor-implementation
            prototype-header
            prototype-implementation
        )

        # FIXME: Instead of requiring a manual declaration of iterable bindings, we should ask BindingsGenerator if it's iterable
        if(LIBWEB_BINDINGS_ITERABLE)
            list(APPEND BINDINGS_SOURCES
                "${LIBWEB_OUTPUT_FOLDER}Bindings/${basename}IteratorPrototype.h"
                "${LIBWEB_OUTPUT_FOLDER}Bindings/${basename}IteratorPrototype.cpp"
            )
            list(APPEND BINDINGS_TYPES
                iterator-prototype-header
                iterator-prototype-implementation
            )
        endif()
        target_sources(${target} PRIVATE ${BINDINGS_SOURCES})
        # FIXME: cmake_minimum_required(3.17) for ZIP_LISTS
        list(LENGTH BINDINGS_SOURCES num_bindings)
        math(EXPR bindings_end "${num_bindings} - 1")
        foreach(iter RANGE "${bindings_end}")
            list(GET BINDINGS_SOURCES ${iter} bindings_src)
            list(GET BINDINGS_TYPES ${iter} bindings_type)
            get_property(include_paths DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR} PROPERTY INCLUDE_DIRECTORIES)
            list(TRANSFORM include_paths PREPEND -i)
            add_custom_command(
                OUTPUT "${bindings_src}"
                COMMAND "$<TARGET_FILE:Lagom::BindingsGenerator>" "--${bindings_type}" ${include_paths} "${LIBWEB_INPUT_FOLDER}/${class}.idl" "${LIBWEB_INPUT_FOLDER}" > "${bindings_src}.tmp"
                COMMAND "${CMAKE_COMMAND}" -E copy_if_different "${bindings_src}.tmp" "${bindings_src}"
                COMMAND "${CMAKE_COMMAND}" -E remove "${bindings_src}.tmp"
                VERBATIM
                DEPENDS Lagom::BindingsGenerator
                MAIN_DEPENDENCY ${LIBWEB_INPUT_FOLDER}/${class}.idl
            )
        endforeach()
        add_custom_target(generate_${basename}Constructor.h DEPENDS ${LIBWEB_OUTPUT_FOLDER}Bindings/${basename}Constructor.h)
        add_dependencies(all_generated generate_${basename}Constructor.h)
        add_custom_target(generate_${basename}Constructor.cpp DEPENDS ${LIBWEB_OUTPUT_FOLDER}Bindings/${basename}Constructor.cpp)
        add_dependencies(all_generated generate_${basename}Constructor.cpp)
        add_custom_target(generate_${basename}Prototype.h DEPENDS ${LIBWEB_OUTPUT_FOLDER}Bindings/${basename}Prototype.h)
        add_dependencies(all_generated generate_${basename}Prototype.h)
        add_custom_target(generate_${basename}Prototype.cpp DEPENDS ${LIBWEB_OUTPUT_FOLDER}Bindings/${basename}Prototype.cpp)
        add_dependencies(all_generated generate_${basename}Prototype.cpp)
    endfunction()

    include("${LIBWEB_INPUT_FOLDER}/idl_files.cmake")

endfunction()
