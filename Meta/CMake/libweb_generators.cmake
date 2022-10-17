function (generate_css_implementation)
    set(LIBWEB_INPUT_FOLDER "${CMAKE_CURRENT_SOURCE_DIR}")
    invoke_generator(
        "Enums.cpp"
        Lagom::GenerateCSSEnums
        "${LIBWEB_INPUT_FOLDER}/CSS/Enums.json"
        "CSS/Enums.h"
        "CSS/Enums.cpp"
        arguments -j "${LIBWEB_INPUT_FOLDER}/CSS/Enums.json"
    )

    invoke_generator(
        "MediaFeatureID.cpp"
        Lagom::GenerateCSSMediaFeatureID
        "${LIBWEB_INPUT_FOLDER}/CSS/MediaFeatures.json"
        "CSS/MediaFeatureID.h"
        "CSS/MediaFeatureID.cpp"
        arguments -j "${LIBWEB_INPUT_FOLDER}/CSS/MediaFeatures.json"
    )

    invoke_generator(
        "PropertyID.cpp"
        Lagom::GenerateCSSPropertyID
        "${LIBWEB_INPUT_FOLDER}/CSS/Properties.json"
        "CSS/PropertyID.h"
        "CSS/PropertyID.cpp"
        arguments -j "${LIBWEB_INPUT_FOLDER}/CSS/Properties.json"
    )

    invoke_generator(
        "TransformFunctions.cpp"
        Lagom::GenerateCSSTransformFunctions
        "${LIBWEB_INPUT_FOLDER}/CSS/TransformFunctions.json"
        "CSS/TransformFunctions.h"
        "CSS/TransformFunctions.cpp"
        arguments -j "${LIBWEB_INPUT_FOLDER}/CSS/TransformFunctions.json"
    )

    invoke_generator(
        "ValueID.cpp"
        Lagom::GenerateCSSValueID
        "${LIBWEB_INPUT_FOLDER}/CSS/Identifiers.json"
        "CSS/ValueID.h"
        "CSS/ValueID.cpp"
        arguments -j "${LIBWEB_INPUT_FOLDER}/CSS/Identifiers.json"
    )

    add_custom_command(
        OUTPUT CSS/DefaultStyleSheetSource.cpp
        COMMAND "${CMAKE_COMMAND}" -E make_directory CSS
        COMMAND "${LIBWEB_INPUT_FOLDER}/Scripts/GenerateStyleSheetSource.sh" default_stylesheet_source "${LIBWEB_INPUT_FOLDER}/CSS/Default.css" > CSS/DefaultStyleSheetSource.cpp.tmp
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different CSS/DefaultStyleSheetSource.cpp.tmp CSS/DefaultStyleSheetSource.cpp
        COMMAND "${CMAKE_COMMAND}" -E remove CSS/DefaultStyleSheetSource.cpp.tmp
        VERBATIM
        DEPENDS "${LIBWEB_INPUT_FOLDER}/Scripts/GenerateStyleSheetSource.sh"
        MAIN_DEPENDENCY "${LIBWEB_INPUT_FOLDER}/CSS/Default.css"
    )
    add_custom_target(generate_DefaultStyleSheetSource.cpp DEPENDS CSS/DefaultStyleSheetSource.cpp)
    add_dependencies(all_generated generate_DefaultStyleSheetSource.cpp)

    add_custom_command(
        OUTPUT CSS/QuirksModeStyleSheetSource.cpp
        COMMAND "${CMAKE_COMMAND}" -E make_directory CSS
        COMMAND "${LIBWEB_INPUT_FOLDER}/Scripts/GenerateStyleSheetSource.sh" quirks_mode_stylesheet_source "${LIBWEB_INPUT_FOLDER}/CSS/QuirksMode.css" > CSS/QuirksModeStyleSheetSource.cpp.tmp
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different CSS/QuirksModeStyleSheetSource.cpp.tmp CSS/QuirksModeStyleSheetSource.cpp
        COMMAND "${CMAKE_COMMAND}" -E remove CSS/QuirksModeStyleSheetSource.cpp.tmp
        VERBATIM
        DEPENDS "${LIBWEB_INPUT_FOLDER}/Scripts/GenerateStyleSheetSource.sh"
        MAIN_DEPENDENCY "${LIBWEB_INPUT_FOLDER}/CSS/Default.css"
    )
    add_custom_target(generate_QuirksModeStyleSheetSource.cpp DEPENDS CSS/QuirksModeStyleSheetSource.cpp)
    add_dependencies(all_generated generate_QuirksModeStyleSheetSource.cpp)

endfunction()

function (generate_js_bindings target)
    set(LIBWEB_INPUT_FOLDER "${CMAKE_CURRENT_SOURCE_DIR}")
    function(libweb_js_bindings class)
        cmake_parse_arguments(PARSE_ARGV 1 LIBWEB_BINDINGS "ITERABLE" "" "")
        get_filename_component(basename "${class}" NAME)
        set(BINDINGS_SOURCES
            "Bindings/${basename}Constructor.h"
            "Bindings/${basename}Constructor.cpp"
            "Bindings/${basename}Prototype.h"
            "Bindings/${basename}Prototype.cpp"
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
                "Bindings/${basename}IteratorPrototype.h"
                "Bindings/${basename}IteratorPrototype.cpp"
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
                MAIN_DEPENDENCY ${class}.idl
            )
        endforeach()

        foreach(generated_file IN LISTS BINDINGS_SOURCES)
            get_filename_component(generated_name ${generated_file} NAME)
            add_custom_target(generate_${generated_name} DEPENDS ${generated_file})
            add_dependencies(all_generated generate_${generated_name})
            add_dependencies(${target} generate_${generated_name})
        endforeach()

        list(APPEND LIBWEB_ALL_IDL_FILES "${LIBWEB_INPUT_FOLDER}/${class}.idl")
        set(LIBWEB_ALL_IDL_FILES ${LIBWEB_ALL_IDL_FILES} PARENT_SCOPE)
    endfunction()

    function(generate_exposed_interface_files)
        set(exposed_interface_sources DedicatedWorkerExposedInterfaces.cpp DedicatedWorkerExposedInterfaces.h
        SharedWorkerExposedInterfaces.cpp SharedWorkerExposedInterfaces.h
        WindowExposedInterfaces.cpp WindowExposedInterfaces.h)
        list(TRANSFORM exposed_interface_sources PREPEND "Bindings/")
        add_custom_command(
            OUTPUT  ${exposed_interface_sources}
            COMMAND "${CMAKE_COMMAND}" -E make_directory "tmp"
            COMMAND $<TARGET_FILE:Lagom::GenerateWindowOrWorkerInterfaces> -o "${CMAKE_CURRENT_BINARY_DIR}/tmp" -b "${LIBWEB_INPUT_FOLDER}" ${LIBWEB_ALL_IDL_FILES}
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different tmp/DedicatedWorkerExposedInterfaces.h "Bindings/DedicatedWorkerExposedInterfaces.h"
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different tmp/DedicatedWorkerExposedInterfaces.cpp "Bindings/DedicatedWorkerExposedInterfaces.cpp"
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different tmp/SharedWorkerExposedInterfaces.h "Bindings/SharedWorkerExposedInterfaces.h"
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different tmp/SharedWorkerExposedInterfaces.cpp "Bindings/SharedWorkerExposedInterfaces.cpp"
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different tmp/WindowExposedInterfaces.h "Bindings/WindowExposedInterfaces.h"
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different tmp/WindowExposedInterfaces.cpp "Bindings/WindowExposedInterfaces.cpp"
            COMMAND "${CMAKE_COMMAND}" -E remove_directory "${CMAKE_CURRENT_BINARY_DIR}/tmp"
            VERBATIM
            DEPENDS Lagom::GenerateWindowOrWorkerInterfaces ${LIBWEB_ALL_IDL_FILES}
        )
        target_sources(${target} PRIVATE ${exposed_interface_sources})
        add_custom_target(generate_exposed_interfaces DEPENDS ${exposed_interface_sources})
        add_dependencies(all_generated generate_exposed_interfaces)
        add_dependencies(${target} generate_exposed_interfaces)
    endfunction()

    include("idl_files.cmake")
    generate_exposed_interface_files()

endfunction()
