include(${CMAKE_CURRENT_LIST_DIR}/utils.cmake)

set(UCD_VERSION "15.1.0")
set(UCD_SHA256 "cb1c663d053926500cd501229736045752713a066bd75802098598b7a7056177")
set(EMOJI_SHA256 "d876ee249aa28eaa76cfa6dfaa702847a8d13b062aa488d465d0395ee8137ed9")
set(IDNA_SHA256 "402cbd285f1f952fcd0834b63541d54f69d3d8f1b8f8599bf71a1a14935f82c4")

set(UCD_PATH "${SERENITY_CACHE_DIR}/UCD" CACHE PATH "Download location for UCD files")
set(UCD_VERSION_FILE "${UCD_PATH}/version.txt")

set(UCD_ZIP_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/UCD.zip")
set(UCD_ZIP_PATH "${UCD_PATH}/UCD.zip")

set(UNICODE_DATA_SOURCE "UnicodeData.txt")
set(UNICODE_DATA_PATH "${UCD_PATH}/${UNICODE_DATA_SOURCE}")

set(SPECIAL_CASING_SOURCE "SpecialCasing.txt")
set(SPECIAL_CASING_PATH "${UCD_PATH}/${SPECIAL_CASING_SOURCE}")

set(CASE_FOLDING_SOURCE "CaseFolding.txt")
set(CASE_FOLDING_PATH "${UCD_PATH}/${CASE_FOLDING_SOURCE}")

set(DERIVED_GENERAL_CATEGORY_SOURCE "extracted/DerivedGeneralCategory.txt")
set(DERIVED_GENERAL_CATEGORY_PATH "${UCD_PATH}/${DERIVED_GENERAL_CATEGORY_SOURCE}")

set(PROP_LIST_SOURCE "PropList.txt")
set(PROP_LIST_PATH "${UCD_PATH}/${PROP_LIST_SOURCE}")

set(DERIVED_CORE_PROP_SOURCE "DerivedCoreProperties.txt")
set(DERIVED_CORE_PROP_PATH "${UCD_PATH}/${DERIVED_CORE_PROP_SOURCE}")

set(DERIVED_BINARY_PROP_SOURCE "extracted/DerivedBinaryProperties.txt")
set(DERIVED_BINARY_PROP_PATH "${UCD_PATH}/${DERIVED_BINARY_PROP_SOURCE}")

set(PROP_ALIAS_SOURCE "PropertyAliases.txt")
set(PROP_ALIAS_PATH "${UCD_PATH}/${PROP_ALIAS_SOURCE}")

set(PROP_VALUE_ALIAS_SOURCE "PropertyValueAliases.txt")
set(PROP_VALUE_ALIAS_PATH "${UCD_PATH}/${PROP_VALUE_ALIAS_SOURCE}")

set(NAME_ALIAS_SOURCE "NameAliases.txt")
set(NAME_ALIAS_PATH "${UCD_PATH}/${NAME_ALIAS_SOURCE}")

set(SCRIPTS_SOURCE "Scripts.txt")
set(SCRIPTS_PATH "${UCD_PATH}/${SCRIPTS_SOURCE}")

set(SCRIPT_EXTENSIONS_SOURCE "ScriptExtensions.txt")
set(SCRIPT_EXTENSIONS_PATH "${UCD_PATH}/${SCRIPT_EXTENSIONS_SOURCE}")

set(BLOCKS_SOURCE "Blocks.txt")
set(BLOCKS_PATH "${UCD_PATH}/${BLOCKS_SOURCE}")

set(EMOJI_DATA_SOURCE "emoji/emoji-data.txt")
set(EMOJI_DATA_PATH "${UCD_PATH}/${EMOJI_DATA_SOURCE}")

set(NORM_PROPS_SOURCE "DerivedNormalizationProps.txt")
set(NORM_PROPS_PATH "${UCD_PATH}/${NORM_PROPS_SOURCE}")

set(GRAPHEME_BREAK_PROP_SOURCE "auxiliary/GraphemeBreakProperty.txt")
set(GRAPHEME_BREAK_PROP_PATH "${UCD_PATH}/${GRAPHEME_BREAK_PROP_SOURCE}")

set(WORD_BREAK_PROP_SOURCE "auxiliary/WordBreakProperty.txt")
set(WORD_BREAK_PROP_PATH "${UCD_PATH}/${WORD_BREAK_PROP_SOURCE}")

set(SENTENCE_BREAK_PROP_SOURCE "auxiliary/SentenceBreakProperty.txt")
set(SENTENCE_BREAK_PROP_PATH "${UCD_PATH}/${SENTENCE_BREAK_PROP_SOURCE}")

string(REGEX REPLACE "([0-9]+\\.[0-9]+)\\.[0-9]+" "\\1" EMOJI_VERSION "${UCD_VERSION}")
set(EMOJI_TEST_URL "https://www.unicode.org/Public/emoji/${EMOJI_VERSION}/emoji-test.txt")
set(EMOJI_TEST_PATH "${UCD_PATH}/emoji-test.txt")
set(EMOJI_RES_PATH "${SerenityOS_SOURCE_DIR}/Base/res/emoji")
set(EMOJI_SERENITY_PATH "${SerenityOS_SOURCE_DIR}/Base/home/anon/Documents/emoji-serenity.txt")
set(EMOJI_FILE_LIST_PATH "${SerenityOS_SOURCE_DIR}/Meta/emoji-file-list.txt")
set(EMOJI_INSTALL_PATH "${CMAKE_BINARY_DIR}/Root/home/anon/Documents/emoji.txt")

set(IDNA_MAPPING_TABLE_URL "https://www.unicode.org/Public/idna/${UCD_VERSION}/IdnaMappingTable.txt")
set(IDNA_MAPPING_TABLE_PATH "${UCD_PATH}/IdnaMappingTable.txt")

if (ENABLE_UNICODE_DATABASE_DOWNLOAD)
    remove_path_if_version_changed("${UCD_VERSION}" "${UCD_VERSION_FILE}" "${UCD_PATH}")

    if (ENABLE_NETWORK_DOWNLOADS)
        download_file("${UCD_ZIP_URL}" "${UCD_ZIP_PATH}" SHA256 "${UCD_SHA256}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${UNICODE_DATA_SOURCE}" "${UNICODE_DATA_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${SPECIAL_CASING_SOURCE}" "${SPECIAL_CASING_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${CASE_FOLDING_SOURCE}" "${CASE_FOLDING_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${DERIVED_GENERAL_CATEGORY_SOURCE}" "${DERIVED_GENERAL_CATEGORY_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${PROP_LIST_SOURCE}" "${PROP_LIST_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${DERIVED_CORE_PROP_SOURCE}" "${DERIVED_CORE_PROP_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${DERIVED_BINARY_PROP_SOURCE}" "${DERIVED_BINARY_PROP_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${PROP_ALIAS_SOURCE}" "${PROP_ALIAS_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${PROP_VALUE_ALIAS_SOURCE}" "${PROP_VALUE_ALIAS_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${NAME_ALIAS_SOURCE}" "${NAME_ALIAS_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${SCRIPTS_SOURCE}" "${SCRIPTS_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${SCRIPT_EXTENSIONS_SOURCE}" "${SCRIPT_EXTENSIONS_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${BLOCKS_SOURCE}" "${BLOCKS_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${EMOJI_DATA_SOURCE}" "${EMOJI_DATA_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${NORM_PROPS_SOURCE}" "${NORM_PROPS_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${GRAPHEME_BREAK_PROP_SOURCE}" "${GRAPHEME_BREAK_PROP_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${WORD_BREAK_PROP_SOURCE}" "${WORD_BREAK_PROP_PATH}")
        extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${SENTENCE_BREAK_PROP_SOURCE}" "${SENTENCE_BREAK_PROP_PATH}")

        download_file("${EMOJI_TEST_URL}" "${EMOJI_TEST_PATH}" SHA256 "${EMOJI_SHA256}")

        download_file("${IDNA_MAPPING_TABLE_URL}" "${IDNA_MAPPING_TABLE_PATH}" SHA256 "${IDNA_SHA256}")
    else()
        message(STATUS "Skipping download of ${UCD_ZIP_URL}, expecting the archive to have been extracted to ${UCD_ZIP_PATH}")
        message(STATUS "Skipping download of ${EMOJI_TEST_URL}, expecting the file to be at ${EMOJI_TEST_PATH}")
        message(STATUS "Skipping download of ${IDNA_MAPPING_TABLE_URL}, expecting the file to be at  ${IDNA_MAPPING_TABLE_PATH}")
    endif()


    set(UNICODE_DATA_HEADER UnicodeData.h)
    set(UNICODE_DATA_IMPLEMENTATION UnicodeData.cpp)

    set(EMOJI_DATA_HEADER EmojiData.h)
    set(EMOJI_DATA_IMPLEMENTATION EmojiData.cpp)

    set(IDNA_DATA_HEADER IDNAData.h)
    set(IDNA_DATA_IMPLEMENTATION IDNAData.cpp)

    if (SERENITYOS)
        set(EMOJI_INSTALL_ARG -i "${EMOJI_INSTALL_PATH}")
    endif()

    invoke_generator(
        "UnicodeData"
        Lagom::GenerateUnicodeData
        "${UCD_VERSION_FILE}"
        "${UNICODE_DATA_HEADER}"
        "${UNICODE_DATA_IMPLEMENTATION}"
        arguments -u "${UNICODE_DATA_PATH}" -s "${SPECIAL_CASING_PATH}" -o "${CASE_FOLDING_PATH}" -g "${DERIVED_GENERAL_CATEGORY_PATH}" -p "${PROP_LIST_PATH}" -d "${DERIVED_CORE_PROP_PATH}" -b "${DERIVED_BINARY_PROP_PATH}" -a "${PROP_ALIAS_PATH}" -v "${PROP_VALUE_ALIAS_PATH}" -r "${SCRIPTS_PATH}" -x "${SCRIPT_EXTENSIONS_PATH}" -k "${BLOCKS_PATH}" -e "${EMOJI_DATA_PATH}" -m "${NAME_ALIAS_PATH}" -n "${NORM_PROPS_PATH}" -f "${GRAPHEME_BREAK_PROP_PATH}" -w "${WORD_BREAK_PROP_PATH}" -i "${SENTENCE_BREAK_PROP_PATH}"
    )
    invoke_generator(
        "EmojiData"
        Lagom::GenerateEmojiData
        "${UCD_VERSION_FILE}"
        "${EMOJI_DATA_HEADER}"
        "${EMOJI_DATA_IMPLEMENTATION}"
        arguments "${EMOJI_INSTALL_ARG}" -e "${EMOJI_TEST_PATH}" -s "${EMOJI_SERENITY_PATH}" -f "${EMOJI_FILE_LIST_PATH}" -r "${EMOJI_RES_PATH}"

        # This will make this command only run when the modified time of the directory changes,
        # which only happens if files within it are added or deleted, but not when a file is modified.
        # This is fine for this use-case, because the contents of a file changing should not affect
        # the generated emoji.txt file.
        dependencies "${EMOJI_RES_PATH}" "${EMOJI_SERENITY_PATH}" "${EMOJI_FILE_LIST_PATH}"
    )
    invoke_generator(
        "IDNAData"
        Lagom::GenerateIDNAData
        "${UCD_VERSION_FILE}"
        "${IDNA_DATA_HEADER}"
        "${IDNA_DATA_IMPLEMENTATION}"
        arguments -m "${IDNA_MAPPING_TABLE_PATH}"
    )

    set(UNICODE_DATA_SOURCES
        ${UNICODE_DATA_HEADER}
        ${UNICODE_DATA_IMPLEMENTATION}
        ${EMOJI_DATA_HEADER}
        ${EMOJI_DATA_IMPLEMENTATION}
        ${IDNA_DATA_HEADER}
        ${IDNA_DATA_IMPLEMENTATION}
    )
endif()
