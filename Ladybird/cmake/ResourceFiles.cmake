set(FONTS
    CsillaBold10.font
    CsillaBold12.font
    CsillaRegular10.font
    CsillaRegular12.font
    KaticaBold10.font
    KaticaBold12.font
    KaticaBoldOblique10.font
    KaticaItalic10.font
    KaticaRegular10.font
    KaticaRegular12.font
    SerenitySans-Regular.ttf
)
list(TRANSFORM FONTS PREPEND "${SERENITY_SOURCE_DIR}/Base/res/fonts/")

set(16x16_ICONS
    app-browser.png
    audio-volume-high.png
    audio-volume-muted.png
    close-tab.png
    edit-copy.png
    filetype-css.png
    filetype-folder-open.png
    filetype-html.png
    filetype-image.png
    filetype-sound.png
    filetype-video.png
    find.png
    go-forward.png
    history.png
    layers.png
    layout.png
    new-tab.png
    open-parent-directory.png
    pause.png
    play.png
    select-all.png
    settings.png
    spoof.png
    trash-can.png
    zoom-in.png
    zoom-out.png
    zoom-reset.png
)
set(32x32_ICONS
    app-browser.png
    filetype-folder.png
    filetype-unknown.png
    msgbox-warning.png
)
set(BROWSER_ICONS
    clear-cache.png
    cookie.png
    dom-tree.png
    local-storage.png
)
list(TRANSFORM 16x16_ICONS PREPEND "${SERENITY_SOURCE_DIR}/Base/res/icons/16x16/")
list(TRANSFORM 32x32_ICONS PREPEND "${SERENITY_SOURCE_DIR}/Base/res/icons/32x32/")
list(TRANSFORM BROWSER_ICONS PREPEND "${SERENITY_SOURCE_DIR}/Base/res/icons/browser/")

set(WEB_RESOURCES
    about.html
    inspector.css
    inspector.js
    newtab.html
)
set(WEB_TEMPLATES
    directory.html
    error.html
    version.html
)
list(TRANSFORM WEB_RESOURCES PREPEND "${SERENITY_SOURCE_DIR}/Base/res/ladybird/")
list(TRANSFORM WEB_TEMPLATES PREPEND "${SERENITY_SOURCE_DIR}/Base/res/ladybird/templates/")

set(THEMES
    Default.ini
    Dark.ini
)
list(TRANSFORM THEMES PREPEND "${SERENITY_SOURCE_DIR}/Base/res/themes/")

set(CONFIG_RESOURCES
    BrowserAutoplayAllowlist.txt
    BrowserContentFilters.txt
)
list(TRANSFORM CONFIG_RESOURCES PREPEND "${SERENITY_SOURCE_DIR}/Base/home/anon/.config/")

set(DOWNLOADED_RESOURCES
    cacert.pem
)
list(TRANSFORM DOWNLOADED_RESOURCES PREPEND "${Lagom_BINARY_DIR}/")

function(copy_resource_set subdir)
    cmake_parse_arguments(PARSE_ARGV 1 "COPY" "" "TARGET;DESTINATION" "RESOURCES")
    set(inputs ${COPY_RESOURCES})

    if (APPLE)
        target_sources(${COPY_TARGET} PRIVATE ${inputs})
        set_source_files_properties(${inputs} PROPERTIES MACOSX_PACKAGE_LOCATION "Resources/${subdir}")
    else()
        set(outputs "")
        foreach (input IN LISTS inputs)
            get_filename_component(input_name "${input}" NAME)
            list(APPEND outputs "${COPY_DESTINATION}/${subdir}/${input_name}")
        endforeach()

        add_custom_command(
            OUTPUT ${outputs}
            COMMAND "${CMAKE_COMMAND}" -E make_directory "${COPY_DESTINATION}/${subdir}"
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different "${inputs}" "${COPY_DESTINATION}/${subdir}"
            COMMAND_EXPAND_LISTS
            VERBATIM
        )
        string(REPLACE "/" "_" subdir_underscores "${subdir}")
        set(target_name "copy_${subdir_underscores}_resources_to_build")
        while (TARGET ${target_name})
            set(target_name "${target_name}_")
        endwhile()
        add_custom_target(${target_name} DEPENDS ${outputs})
        add_dependencies(all_generated ${target_name})
        add_dependencies("${COPY_TARGET}_build_resource_files" ${target_name})
    endif()
endfunction()

function(copy_resources_to_build base_directory bundle_target)

    add_custom_target("${bundle_target}_build_resource_files")

    copy_resource_set(fonts RESOURCES ${FONTS}
        DESTINATION ${base_directory} TARGET ${bundle_target}
    )

    copy_resource_set(icons/16x16 RESOURCES ${16x16_ICONS}
        DESTINATION ${base_directory} TARGET ${bundle_target}
    )

    copy_resource_set(icons/32x32 RESOURCES ${32x32_ICONS}
        DESTINATION ${base_directory} TARGET ${bundle_target}
    )

    copy_resource_set(icons/browser RESOURCES ${BROWSER_ICONS}
        DESTINATION ${base_directory} TARGET ${bundle_target}
    )

    copy_resource_set(themes RESOURCES ${THEMES}
        DESTINATION ${base_directory} TARGET ${bundle_target}
    )

    copy_resource_set(ladybird RESOURCES ${WEB_RESOURCES}
        DESTINATION ${base_directory} TARGET ${bundle_target}
    )

    copy_resource_set(ladybird/templates RESOURCES ${WEB_TEMPLATES}
        DESTINATION ${base_directory} TARGET ${bundle_target}
    )

    copy_resource_set(ladybird RESOURCES ${CONFIG_RESOURCES}
        DESTINATION ${base_directory} TARGET ${bundle_target}
    )

    copy_resource_set(ladybird RESOURCES ${DOWNLOADED_RESOURCES}
        DESTINATION ${base_directory} TARGET ${bundle_target}
    )

    add_dependencies(${bundle_target} "${bundle_target}_build_resource_files")
endfunction()

function(install_ladybird_resources destination component)
    install(FILES ${FONTS} DESTINATION "${destination}/fonts" COMPONENT ${component})
    install(FILES ${16x16_ICONS} DESTINATION "${destination}/icons/16x16" COMPONENT ${component})
    install(FILES ${32x32_ICONS} DESTINATION "${destination}/icons/32x32" COMPONENT ${component})
    install(FILES ${BROWSER_ICONS} DESTINATION "${destination}/icons/browser" COMPONENT ${component})
    install(FILES ${THEMES} DESTINATION "${destination}/themes" COMPONENT ${component})
    install(FILES ${WEB_RESOURCES} DESTINATION "${destination}/ladybird" COMPONENT ${component})
    install(FILES ${WEB_TEMPLATES} DESTINATION "${destination}/ladybird/templates" COMPONENT ${component})
    install(FILES ${CONFIG_RESOURCES} DESTINATION "${destination}/ladybird" COMPONENT ${component})
    install(FILES ${DOWNLOADED_RESOURCES} DESTINATION "${destination}/ladybird" COMPONENT ${component})
endfunction()
