set(UCD_VERSION 13.0.0)
set(CLDR_VERSION 39.0.0)

set(UCD_PATH "${CMAKE_BINARY_DIR}/UCD" CACHE PATH "Download location for UCD files")
set(CLDR_PATH "${CMAKE_BINARY_DIR}/CLDR" CACHE PATH "Download location for CLDR files")

set(UCD_VERSION_FILE "${UCD_PATH}/version.txt")
set(CLDR_VERSION_FILE "${CLDR_PATH}/version.txt")

set(UNICODE_DATA_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/UnicodeData.txt")
set(UNICODE_DATA_PATH "${UCD_PATH}/UnicodeData.txt")

set(SPECIAL_CASING_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/SpecialCasing.txt")
set(SPECIAL_CASING_PATH "${UCD_PATH}/SpecialCasing.txt")

set(DERIVED_GENERAL_CATEGORY_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/extracted/DerivedGeneralCategory.txt")
set(DERIVED_GENERAL_CATEGORY_PATH "${UCD_PATH}/DerivedGeneralCategory.txt")

set(PROP_LIST_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/PropList.txt")
set(PROP_LIST_PATH "${UCD_PATH}/PropList.txt")

set(DERIVED_CORE_PROP_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/DerivedCoreProperties.txt")
set(DERIVED_CORE_PROP_PATH "${UCD_PATH}/DerivedCoreProperties.txt")

set(DERIVED_BINARY_PROP_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/extracted/DerivedBinaryProperties.txt")
set(DERIVED_BINARY_PROP_PATH "${UCD_PATH}/DerivedBinaryProperties.txt")

set(PROP_ALIAS_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/PropertyAliases.txt")
set(PROP_ALIAS_PATH "${UCD_PATH}/PropertyAliases.txt")

set(PROP_VALUE_ALIAS_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/PropertyValueAliases.txt")
set(PROP_VALUE_ALIAS_PATH "${UCD_PATH}/PropertyValueAliases.txt")

set(SCRIPTS_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/Scripts.txt")
set(SCRIPTS_PATH "${UCD_PATH}/Scripts.txt")

set(SCRIPT_EXTENSIONS_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/ScriptExtensions.txt")
set(SCRIPT_EXTENSIONS_PATH "${UCD_PATH}/ScriptExtensions.txt")

set(EMOJI_DATA_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/emoji/emoji-data.txt")
set(EMOJI_DATA_PATH "${UCD_PATH}/emoji-data.txt")

set(NORM_PROPS_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/DerivedNormalizationProps.txt")
set(NORM_PROPS_PATH "${UCD_PATH}/DerivedNormalizationProps.txt")

set(CLDR_ZIP_URL "https://github.com/unicode-org/cldr-json/releases/download/${CLDR_VERSION}/cldr-${CLDR_VERSION}-json-modern.zip")
set(CLDR_ZIP_PATH "${CLDR_PATH}/cldr.zip")

set(CLDR_CORE_SOURCE cldr-core)
set(CLDR_CORE_PATH "${CLDR_PATH}/${CLDR_CORE_SOURCE}")

set(CLDR_LOCALES_SOURCE cldr-localenames-modern)
set(CLDR_LOCALES_PATH "${CLDR_PATH}/${CLDR_LOCALES_SOURCE}")

set(CLDR_MISC_SOURCE cldr-misc-modern)
set(CLDR_MISC_PATH "${CLDR_PATH}/${CLDR_MISC_SOURCE}")

set(CLDR_NUMBERS_SOURCE cldr-numbers-modern)
set(CLDR_NUMBERS_PATH "${CLDR_PATH}/${CLDR_NUMBERS_SOURCE}")

function(remove_unicode_data_if_version_changed version version_file cache_path)
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

function(download_ucd_file url path)
    if (NOT EXISTS "${path}")
        get_filename_component(file "${path}" NAME)
        message(STATUS "Downloading UCD ${file} from ${url}...")
        file(DOWNLOAD "${url}" "${path}" INACTIVITY_TIMEOUT 10)
    endif()
endfunction()

function(extract_cldr_file source path)
    if(EXISTS "${CLDR_ZIP_PATH}" AND NOT EXISTS "${path}")
        message(STATUS "Extracting CLDR ${source} from ${CLDR_ZIP_PATH}...")
        execute_process(COMMAND unzip -q "${CLDR_ZIP_PATH}" "${source}/**" -d "${CLDR_PATH}" RESULT_VARIABLE unzip_result)
        if (NOT unzip_result EQUAL 0)
            message(FATAL_ERROR "Failed to unzip ${source} from ${CLDR_ZIP_PATH} with status ${unzip_result}")
        endif()
    endif()
endfunction()

if (ENABLE_UNICODE_DATABASE_DOWNLOAD)
    remove_unicode_data_if_version_changed("${UCD_VERSION}" "${UCD_VERSION_FILE}" "${UCD_PATH}")
    remove_unicode_data_if_version_changed("${CLDR_VERSION}" "${CLDR_VERSION_FILE}" "${CLDR_PATH}")

    download_ucd_file("${UNICODE_DATA_URL}" "${UNICODE_DATA_PATH}")
    download_ucd_file("${SPECIAL_CASING_URL}" "${SPECIAL_CASING_PATH}")
    download_ucd_file("${DERIVED_GENERAL_CATEGORY_URL}" "${DERIVED_GENERAL_CATEGORY_PATH}")
    download_ucd_file("${PROP_LIST_URL}" "${PROP_LIST_PATH}")
    download_ucd_file("${DERIVED_CORE_PROP_URL}" "${DERIVED_CORE_PROP_PATH}")
    download_ucd_file("${DERIVED_BINARY_PROP_URL}" "${DERIVED_BINARY_PROP_PATH}")
    download_ucd_file("${PROP_ALIAS_URL}" "${PROP_ALIAS_PATH}")
    download_ucd_file("${PROP_VALUE_ALIAS_URL}" "${PROP_VALUE_ALIAS_PATH}")
    download_ucd_file("${SCRIPTS_URL}" "${SCRIPTS_PATH}")
    download_ucd_file("${SCRIPT_EXTENSIONS_URL}" "${SCRIPT_EXTENSIONS_PATH}")
    download_ucd_file("${EMOJI_DATA_URL}" "${EMOJI_DATA_PATH}")
    download_ucd_file("${NORM_PROPS_URL}" "${NORM_PROPS_PATH}")

    if (NOT EXISTS "${CLDR_ZIP_PATH}")
        message(STATUS "Downloading CLDR database from ${CLDR_ZIP_URL}...")
        file(DOWNLOAD "${CLDR_ZIP_URL}" "${CLDR_ZIP_PATH}" INACTIVITY_TIMEOUT 10)
    endif()

    extract_cldr_file("${CLDR_CORE_SOURCE}" "${CLDR_CORE_PATH}")
    extract_cldr_file("${CLDR_LOCALES_SOURCE}" "${CLDR_LOCALES_PATH}")
    extract_cldr_file("${CLDR_MISC_SOURCE}" "${CLDR_MISC_PATH}")
    extract_cldr_file("${CLDR_NUMBERS_SOURCE}" "${CLDR_NUMBERS_PATH}")

    set(UNICODE_DATA_HEADER LibUnicode/UnicodeData.h)
    set(UNICODE_DATA_IMPLEMENTATION LibUnicode/UnicodeData.cpp)

    set(UNICODE_LOCALE_HEADER LibUnicode/UnicodeLocale.h)
    set(UNICODE_LOCALE_IMPLEMENTATION LibUnicode/UnicodeLocale.cpp)

    set(UNICODE_META_TARGET_PREFIX LibUnicode_)

    if (CMAKE_CURRENT_BINARY_DIR MATCHES ".*/LibUnicode") # Serenity build.
        set(UNICODE_DATA_HEADER UnicodeData.h)
        set(UNICODE_DATA_IMPLEMENTATION UnicodeData.cpp)

        set(UNICODE_LOCALE_HEADER UnicodeLocale.h)
        set(UNICODE_LOCALE_IMPLEMENTATION UnicodeLocale.cpp)
        set(UNICODE_META_TARGET_PREFIX "")
    endif()

    add_custom_command(
        OUTPUT ${UNICODE_DATA_HEADER} ${UNICODE_DATA_IMPLEMENTATION}
        COMMAND $<TARGET_FILE:Lagom::GenerateUnicodeData> -h ${UNICODE_DATA_HEADER}.tmp -c ${UNICODE_DATA_IMPLEMENTATION}.tmp -u ${UNICODE_DATA_PATH} -s ${SPECIAL_CASING_PATH} -g ${DERIVED_GENERAL_CATEGORY_PATH} -p ${PROP_LIST_PATH} -d ${DERIVED_CORE_PROP_PATH} -b ${DERIVED_BINARY_PROP_PATH} -a ${PROP_ALIAS_PATH} -v ${PROP_VALUE_ALIAS_PATH} -r ${SCRIPTS_PATH} -x ${SCRIPT_EXTENSIONS_PATH} -e ${EMOJI_DATA_PATH} -n ${NORM_PROPS_PATH}
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different ${UNICODE_DATA_HEADER}.tmp ${UNICODE_DATA_HEADER}
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different ${UNICODE_DATA_IMPLEMENTATION}.tmp ${UNICODE_DATA_IMPLEMENTATION}
        COMMAND "${CMAKE_COMMAND}" -E remove ${UNICODE_DATA_HEADER}.tmp ${UNICODE_DATA_IMPLEMENTATION}.tmp
        VERBATIM
        DEPENDS Lagom::GenerateUnicodeData ${UNICODE_DATA_PATH} ${SPECIAL_CASING_PATH} ${DERIVED_GENERAL_CATEGORY_PATH} ${PROP_LIST_PATH} ${DERIVED_CORE_PROP_PATH} ${DERIVED_BINARY_PROP_PATH} ${PROP_ALIAS_PATH} ${PROP_VALUE_ALIAS_PATH} ${SCRIPTS_PATH} ${SCRIPT_EXTENSIONS_PATH} ${EMOJI_DATA_PATH} ${NORM_PROPS_PATH}
    )
    add_custom_target(generate_${UNICODE_META_TARGET_PREFIX}UnicodeData DEPENDS ${UNICODE_DATA_HEADER} ${UNICODE_DATA_IMPLEMENTATION})
    add_dependencies(all_generated generate_${UNICODE_META_TARGET_PREFIX}UnicodeData)

    add_custom_command(
        OUTPUT ${UNICODE_LOCALE_HEADER} ${UNICODE_LOCALE_IMPLEMENTATION}
        COMMAND $<TARGET_FILE:Lagom::GenerateUnicodeLocale> -h ${UNICODE_LOCALE_HEADER}.tmp -c ${UNICODE_LOCALE_IMPLEMENTATION}.tmp -r ${CLDR_CORE_PATH} -l ${CLDR_LOCALES_PATH} -m ${CLDR_MISC_PATH} -n ${CLDR_NUMBERS_PATH}
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different ${UNICODE_LOCALE_HEADER}.tmp ${UNICODE_LOCALE_HEADER}
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different ${UNICODE_LOCALE_IMPLEMENTATION}.tmp ${UNICODE_LOCALE_IMPLEMENTATION}
        COMMAND "${CMAKE_COMMAND}" -E remove ${UNICODE_LOCALE_HEADER}.tmp ${UNICODE_LOCALE_IMPLEMENTATION}.tmp
        VERBATIM
        DEPENDS Lagom::GenerateUnicodeLocale ${CLDR_CORE_PATH} ${CLDR_LOCALES_PATH} ${CLDR_MISC_PATH} ${CLDR_NUMBERS_PATH}
    )
    add_custom_target(generate_${UNICODE_META_TARGET_PREFIX}UnicodeLocale DEPENDS ${UNICODE_LOCALE_HEADER} ${UNICODE_LOCALE_IMPLEMENTATION})
    add_dependencies(all_generated generate_${UNICODE_META_TARGET_PREFIX}UnicodeLocale)

    set(UNICODE_DATA_SOURCES ${UNICODE_DATA_HEADER} ${UNICODE_DATA_IMPLEMENTATION} ${UNICODE_LOCALE_HEADER} ${UNICODE_LOCALE_IMPLEMENTATION})
endif()
