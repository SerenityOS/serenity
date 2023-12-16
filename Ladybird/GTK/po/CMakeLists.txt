find_package(Gettext)
find_program(XGETTEXT NAMES xgettext)

if (GETTEXT_FOUND AND XGETTEXT)
    set(pot_file "${CMAKE_CURRENT_BINARY_DIR}/ladybird-gtk4.pot")
    add_custom_command(
        OUTPUT "${pot_file}"
        COMMAND "${XGETTEXT}"
        ARGS
            --files-from "${CMAKE_CURRENT_SOURCE_DIR}/POTFILES.in"
            --directory "${CMAKE_CURRENT_SOURCE_DIR}/.."
            --language C++
            --output "${pot_file}"
            --from-code=UTF-8
            --add-comments="Translators"
            --keyword="_"
            --keyword="N_"
            --keyword="C_:1c,2"
            --keyword="NC_:1c,2"
        DEPENDS POTFILES.in ladybird
    )

    set(linguas
        eo.po
        fr.po
        ru.po
        sv.po
    )
    GETTEXT_CREATE_TRANSLATIONS("${pot_file}" ALL "${linguas}")
endif()
