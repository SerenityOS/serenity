include(${CMAKE_CURRENT_LIST_DIR}/utils.cmake)

set(UCD_VERSION 14.0.0)
set(CLDR_VERSION 41.0.0)

set(UCD_PATH "${CMAKE_BINARY_DIR}/UCD" CACHE PATH "Download location for UCD files")
set(CLDR_PATH "${CMAKE_BINARY_DIR}/CLDR" CACHE PATH "Download location for CLDR files")

set(UCD_VERSION_FILE "${UCD_PATH}/version.txt")
set(CLDR_VERSION_FILE "${CLDR_PATH}/version.txt")

set(UCD_ZIP_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/UCD.zip")
set(UCD_ZIP_PATH "${UCD_PATH}/UCD.zip")

set(UNICODE_DATA_SOURCE "UnicodeData.txt")
set(UNICODE_DATA_PATH "${UCD_PATH}/${UNICODE_DATA_SOURCE}")

set(SPECIAL_CASING_SOURCE "SpecialCasing.txt")
set(SPECIAL_CASING_PATH "${UCD_PATH}/${SPECIAL_CASING_SOURCE}")

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
set(EMOJI_TEST_URL "https://unicode.org/Public/emoji/${EMOJI_VERSION}/emoji-test.txt")
set(EMOJI_TEST_PATH "${UCD_PATH}/emoji-test.txt")
set(EMOJI_GENERATOR_PATH "${SerenityOS_SOURCE_DIR}/Meta/generate-emoji-txt.sh")
set(EMOJI_RES_PATH "${SerenityOS_SOURCE_DIR}/Base/res/emoji")
set(EMOJI_INSTALL_PATH "${SerenityOS_SOURCE_DIR}/Base/home/anon/Documents/emoji.txt")

set(CLDR_ZIP_URL "https://github.com/unicode-org/cldr-json/releases/download/${CLDR_VERSION}/cldr-${CLDR_VERSION}-json-modern.zip")
set(CLDR_ZIP_PATH "${CLDR_PATH}/cldr.zip")

set(CLDR_BCP47_SOURCE cldr-bcp47)
set(CLDR_BCP47_PATH "${CLDR_PATH}/${CLDR_BCP47_SOURCE}")

set(CLDR_CORE_SOURCE cldr-core)
set(CLDR_CORE_PATH "${CLDR_PATH}/${CLDR_CORE_SOURCE}")

set(CLDR_DATES_SOURCE cldr-dates-modern)
set(CLDR_DATES_PATH "${CLDR_PATH}/${CLDR_DATES_SOURCE}")

set(CLDR_LOCALES_SOURCE cldr-localenames-modern)
set(CLDR_LOCALES_PATH "${CLDR_PATH}/${CLDR_LOCALES_SOURCE}")

set(CLDR_MISC_SOURCE cldr-misc-modern)
set(CLDR_MISC_PATH "${CLDR_PATH}/${CLDR_MISC_SOURCE}")

set(CLDR_NUMBERS_SOURCE cldr-numbers-modern)
set(CLDR_NUMBERS_PATH "${CLDR_PATH}/${CLDR_NUMBERS_SOURCE}")

set(CLDR_UNITS_SOURCE cldr-units-modern)
set(CLDR_UNITS_PATH "${CLDR_PATH}/${CLDR_UNITS_SOURCE}")

function(extract_path dest_dir zip_path source_path dest_path)
    if (EXISTS "${zip_path}" AND NOT EXISTS "${dest_path}")
        message(STATUS "Extracting ${source_path} from ${zip_path}")
        execute_process(COMMAND "${UNZIP_TOOL}" -q "${zip_path}" "${source_path}" -d "${dest_dir}" RESULT_VARIABLE unzip_result)
        if (NOT unzip_result EQUAL 0)
            message(FATAL_ERROR "Failed to unzip ${source_path} from ${zip_path} with status ${unzip_result}")
        endif()
    endif()
endfunction()

if (ENABLE_UNICODE_DATABASE_DOWNLOAD)
    remove_path_if_version_changed("${UCD_VERSION}" "${UCD_VERSION_FILE}" "${UCD_PATH}")
    remove_path_if_version_changed("${CLDR_VERSION}" "${CLDR_VERSION_FILE}" "${CLDR_PATH}")

    download_file("${UCD_ZIP_URL}" "${UCD_ZIP_PATH}")
    extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${UNICODE_DATA_SOURCE}" "${UNICODE_DATA_PATH}")
    extract_path("${UCD_PATH}" "${UCD_ZIP_PATH}" "${SPECIAL_CASING_SOURCE}" "${SPECIAL_CASING_PATH}")
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

    download_file("${EMOJI_TEST_URL}" "${EMOJI_TEST_PATH}")

    download_file("${CLDR_ZIP_URL}" "${CLDR_ZIP_PATH}")
    extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_BCP47_SOURCE}/**" "${CLDR_BCP47_PATH}")
    extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_CORE_SOURCE}/**" "${CLDR_CORE_PATH}")
    extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_DATES_SOURCE}/**" "${CLDR_DATES_PATH}")
    extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_LOCALES_SOURCE}/**" "${CLDR_LOCALES_PATH}")
    extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_MISC_SOURCE}/**" "${CLDR_MISC_PATH}")
    extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_NUMBERS_SOURCE}/**" "${CLDR_NUMBERS_PATH}")
    extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_UNITS_SOURCE}/**" "${CLDR_UNITS_PATH}")

    set(UNICODE_DATA_HEADER LibUnicode/UnicodeData.h)
    set(UNICODE_DATA_IMPLEMENTATION LibUnicode/UnicodeData.cpp)

    set(UNICODE_DATE_TIME_FORMAT_HEADER LibUnicode/UnicodeDateTimeFormat.h)
    set(UNICODE_DATE_TIME_FORMAT_IMPLEMENTATION LibUnicode/UnicodeDateTimeFormat.cpp)

    set(UNICODE_LOCALE_HEADER LibUnicode/UnicodeLocale.h)
    set(UNICODE_LOCALE_IMPLEMENTATION LibUnicode/UnicodeLocale.cpp)

    set(UNICODE_NUMBER_FORMAT_HEADER LibUnicode/UnicodeNumberFormat.h)
    set(UNICODE_NUMBER_FORMAT_IMPLEMENTATION LibUnicode/UnicodeNumberFormat.cpp)

    set(UNICODE_PLURAL_RULES_HEADER LibUnicode/UnicodePluralRules.h)
    set(UNICODE_PLURAL_RULES_IMPLEMENTATION LibUnicode/UnicodePluralRules.cpp)

    set(UNICODE_RELATIVE_TIME_FORMAT_HEADER LibUnicode/UnicodeRelativeTimeFormat.h)
    set(UNICODE_RELATIVE_TIME_FORMAT_IMPLEMENTATION LibUnicode/UnicodeRelativeTimeFormat.cpp)

    set(UNICODE_META_TARGET_PREFIX LibUnicode_)

    if (CMAKE_CURRENT_BINARY_DIR MATCHES ".*/LibUnicode") # Serenity build.
        set(UNICODE_DATA_HEADER UnicodeData.h)
        set(UNICODE_DATA_IMPLEMENTATION UnicodeData.cpp)

        set(UNICODE_DATE_TIME_FORMAT_HEADER UnicodeDateTimeFormat.h)
        set(UNICODE_DATE_TIME_FORMAT_IMPLEMENTATION UnicodeDateTimeFormat.cpp)

        set(UNICODE_LOCALE_HEADER UnicodeLocale.h)
        set(UNICODE_LOCALE_IMPLEMENTATION UnicodeLocale.cpp)

        set(UNICODE_NUMBER_FORMAT_HEADER UnicodeNumberFormat.h)
        set(UNICODE_NUMBER_FORMAT_IMPLEMENTATION UnicodeNumberFormat.cpp)

        set(UNICODE_PLURAL_RULES_HEADER UnicodePluralRules.h)
        set(UNICODE_PLURAL_RULES_IMPLEMENTATION UnicodePluralRules.cpp)

        set(UNICODE_RELATIVE_TIME_FORMAT_HEADER UnicodeRelativeTimeFormat.h)
        set(UNICODE_RELATIVE_TIME_FORMAT_IMPLEMENTATION UnicodeRelativeTimeFormat.cpp)

        set(UNICODE_META_TARGET_PREFIX "")
    endif()

    invoke_generator(
        "UnicodeData"
        Lagom::GenerateUnicodeData
        "${UCD_VERSION_FILE}"
        "${UNICODE_META_TARGET_PREFIX}"
        "${UNICODE_DATA_HEADER}"
        "${UNICODE_DATA_IMPLEMENTATION}"
        arguments -u "${UNICODE_DATA_PATH}" -s "${SPECIAL_CASING_PATH}" -g "${DERIVED_GENERAL_CATEGORY_PATH}" -p "${PROP_LIST_PATH}" -d "${DERIVED_CORE_PROP_PATH}" -b "${DERIVED_BINARY_PROP_PATH}" -a "${PROP_ALIAS_PATH}" -v "${PROP_VALUE_ALIAS_PATH}" -r "${SCRIPTS_PATH}" -x "${SCRIPT_EXTENSIONS_PATH}" -k "${BLOCKS_PATH}" -e "${EMOJI_DATA_PATH}" -m "${NAME_ALIAS_PATH}" -n "${NORM_PROPS_PATH}" -f "${GRAPHEME_BREAK_PROP_PATH}" -w "${WORD_BREAK_PROP_PATH}" -i "${SENTENCE_BREAK_PROP_PATH}"
    )
    invoke_generator(
        "UnicodeDateTimeFormat"
        Lagom::GenerateUnicodeDateTimeFormat
        "${CLDR_VERSION_FILE}"
        "${UNICODE_META_TARGET_PREFIX}"
        "${UNICODE_DATE_TIME_FORMAT_HEADER}"
        "${UNICODE_DATE_TIME_FORMAT_IMPLEMENTATION}"
        arguments -r "${CLDR_CORE_PATH}" -d "${CLDR_DATES_PATH}"
    )
    invoke_generator(
        "UnicodeLocale"
        Lagom::GenerateUnicodeLocale
        "${CLDR_VERSION_FILE}"
        "${UNICODE_META_TARGET_PREFIX}"
        "${UNICODE_LOCALE_HEADER}"
        "${UNICODE_LOCALE_IMPLEMENTATION}"
        arguments -b "${CLDR_BCP47_PATH}" -r "${CLDR_CORE_PATH}" -l "${CLDR_LOCALES_PATH}" -m "${CLDR_MISC_PATH}" -n "${CLDR_NUMBERS_PATH}" -d "${CLDR_DATES_PATH}"
    )
    invoke_generator(
        "UnicodeNumberFormat"
        Lagom::GenerateUnicodeNumberFormat
        "${CLDR_VERSION_FILE}"
        "${UNICODE_META_TARGET_PREFIX}"
        "${UNICODE_NUMBER_FORMAT_HEADER}"
        "${UNICODE_NUMBER_FORMAT_IMPLEMENTATION}"
        arguments -r "${CLDR_CORE_PATH}" -n "${CLDR_NUMBERS_PATH}" -u "${CLDR_UNITS_PATH}"
    )
    invoke_generator(
        "UnicodePluralRules"
        Lagom::GenerateUnicodePluralRules
        "${CLDR_VERSION_FILE}"
        "${UNICODE_META_TARGET_PREFIX}"
        "${UNICODE_PLURAL_RULES_HEADER}"
        "${UNICODE_PLURAL_RULES_IMPLEMENTATION}"
        arguments -r "${CLDR_CORE_PATH}" -l "${CLDR_LOCALES_PATH}"
    )
    invoke_generator(
        "UnicodeRelativeTimeFormat"
        Lagom::GenerateUnicodeRelativeTimeFormat
        "${CLDR_VERSION_FILE}"
        "${UNICODE_META_TARGET_PREFIX}"
        "${UNICODE_RELATIVE_TIME_FORMAT_HEADER}"
        "${UNICODE_RELATIVE_TIME_FORMAT_IMPLEMENTATION}"
        arguments -d "${CLDR_DATES_PATH}"
    )

    if (CMAKE_CURRENT_BINARY_DIR MATCHES ".*/LibUnicode") # Serenity build.
        add_custom_command(
            OUTPUT "${EMOJI_INSTALL_PATH}"
            COMMAND "${EMOJI_GENERATOR_PATH}" "${EMOJI_TEST_PATH}" "${EMOJI_RES_PATH}" "${EMOJI_INSTALL_PATH}"
            # This will make this command only run when the modified time of the directory changes,
            # which only happens if files within it are added or deleted, but not when a file is modified.
            # This is fine for this use-case, because the contents of a file changing should not affect
            # the generated emoji.txt file.
            DEPENDS "${EMOJI_GENERATOR_PATH}" "${EMOJI_RES_PATH}" "${EMOJI_TEST_PATH}"
            USES_TERMINAL
        )
        add_custom_target(generate_emoji_txt ALL DEPENDS "${EMOJI_INSTALL_PATH}")
    endif()

    set(UNICODE_DATA_SOURCES
        ${UNICODE_DATA_HEADER}
        ${UNICODE_DATA_IMPLEMENTATION}
        ${UNICODE_DATE_TIME_FORMAT_HEADER}
        ${UNICODE_DATE_TIME_FORMAT_IMPLEMENTATION}
        ${UNICODE_LOCALE_HEADER}
        ${UNICODE_LOCALE_IMPLEMENTATION}
        ${UNICODE_NUMBER_FORMAT_HEADER}
        ${UNICODE_NUMBER_FORMAT_IMPLEMENTATION}
        ${UNICODE_PLURAL_RULES_HEADER}
        ${UNICODE_PLURAL_RULES_IMPLEMENTATION}
        ${UNICODE_RELATIVE_TIME_FORMAT_HEADER}
        ${UNICODE_RELATIVE_TIME_FORMAT_IMPLEMENTATION}
    )
endif()
