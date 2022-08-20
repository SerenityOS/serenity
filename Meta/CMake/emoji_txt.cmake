option(ENABLE_EMOJI_TXT_GENERATION "Enable download of emoji-test.txt and generation of emoji.txt at build time" ON)

set(EMOJI_TEST_TXT_PATH ${CMAKE_BINARY_DIR}/emoji-test.txt)
set(EMOJI_TEST_TXT_URL https://unicode.org/Public/emoji/14.0/emoji-test.txt)

if(ENABLE_EMOJI_TXT_GENERATION)
    if(NOT EXISTS ${EMOJI_TEST_TXT_PATH})
        file(DOWNLOAD ${EMOJI_TEST_TXT_URL} ${EMOJI_TEST_TXT_PATH})
    endif()
    set(EMOJI_RES_PATH "${SerenityOS_SOURCE_DIR}/Base/res/emoji")
    set(EMOJI_TXT_INSTALL_PATH "${SerenityOS_SOURCE_DIR}/Base/home/anon/Documents/emoji.txt")
    add_custom_command(
        OUTPUT ${EMOJI_TXT_INSTALL_PATH}
        COMMAND ${SerenityOS_SOURCE_DIR}/Meta/generate-emoji-txt.sh "${EMOJI_TEST_TXT_PATH}" "${EMOJI_RES_PATH}" "${EMOJI_TXT_INSTALL_PATH}"
        # This will make this command only run when the modified time of the directory changes,
        # which only happens if files within it are added or deleted, but not when a file is modified.
        # This is fine for this use-case, because the contents of a file changing should not affect
        # the generated emoji.txt file.
        MAIN_DEPENDENCY ${EMOJI_RES_PATH}
        USES_TERMINAL
    )
    add_custom_target(generate_emoji_txt ALL DEPENDS ${EMOJI_TXT_INSTALL_PATH})
endif()
