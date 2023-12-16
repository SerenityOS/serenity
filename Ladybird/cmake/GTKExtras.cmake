#
# Additional build rules for GTK resources and blueprints
#

set(RESOURCE_FILES GTK/style.css)
function(add_blueprint source_file ui_file)
    add_custom_command(
            OUTPUT "${ui_file}"
            COMMAND "${BLUEPRINT_COMPILER}"
            ARGS
            compile
            --output "${CMAKE_CURRENT_BINARY_DIR}/${ui_file}"
            "${CMAKE_CURRENT_SOURCE_DIR}/${source_file}"
            DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/${source_file}"
    )
    list(APPEND RESOURCE_FILES "${ui_file}")
    set(RESOURCE_FILES ${RESOURCE_FILES} PARENT_SCOPE)
endfunction()

add_blueprint(GTK/window.blp window.ui)
add_blueprint(GTK/shortcuts-dialog.blp shortcuts-dialog.ui)
add_blueprint(GTK/tab.blp tab.ui)
add_blueprint(GTK/location-entry.blp location-entry.ui)

set(SOURCE_GRESOURCE_FILE "${CMAKE_CURRENT_SOURCE_DIR}/GTK/gresources.xml")
set(GENERATED_GRESOURCE_FILE "${CMAKE_CURRENT_BINARY_DIR}/ladybird-resources.c")

add_custom_command(
        OUTPUT "${GENERATED_GRESOURCE_FILE}"
        COMMAND "${GLIB_COMPILE_RESOURCES}"
        ARGS
        --target "${GENERATED_GRESOURCE_FILE}"
        --generate-source
        --sourcedir "${CMAKE_CURRENT_SOURCE_DIR}/GTK"
        --sourcedir "${CMAKE_CURRENT_BINARY_DIR}"
        "${SOURCE_GRESOURCE_FILE}"
        DEPENDS "${SOURCE_GRESOURCE_FILE}" ${RESOURCE_FILES}
)
add_library(ladybird_gresources OBJECT "${GENERATED_GRESOURCE_FILE}")
target_link_libraries(ladybird_gresources PRIVATE PkgConfig::gtk4)
set_target_properties(ladybird_gresources PROPERTIES COMPILE_OPTIONS "")

# FIXME: Find a way to avoid hard coding the full path to the locale directory
include(GNUInstallDirs)
set_source_files_properties(GTK/main.cpp PROPERTIES COMPILE_DEFINITIONS "LOCALEDIR=\"${CMAKE_INSTALL_FULL_LOCALEDIR}\"")

target_link_libraries(ladybird PRIVATE ladybird_gresources)
