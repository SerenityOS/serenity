set(UCD_VERSION 13.0.0)
set(CLDR_VERSION 39.0.0)

set(LOCALE_DATA_CACHE_LOCATION "${CMAKE_BINARY_DIR}/CLDR" CACHE PATH "Download location for CLDR files")
set(UNICODE_CACHE_LOCATION "${CMAKE_BINARY_DIR}/UCD" CACHE PATH "Download location for UCD files")

set(UNICODE_DATA_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/UnicodeData.txt")
set(UNICODE_DATA_PATH "${UNICODE_CACHE_LOCATION}/UnicodeData.txt")

set(SPECIAL_CASING_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/SpecialCasing.txt")
set(SPECIAL_CASING_PATH "${UNICODE_CACHE_LOCATION}/SpecialCasing.txt")

set(DERIVED_GENERAL_CATEGORY_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/extracted/DerivedGeneralCategory.txt")
set(DERIVED_GENERAL_CATEGORY_PATH "${UNICODE_CACHE_LOCATION}/DerivedGeneralCategory.txt")

set(PROP_LIST_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/PropList.txt")
set(PROP_LIST_PATH "${UNICODE_CACHE_LOCATION}/PropList.txt")

set(DERIVED_CORE_PROP_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/DerivedCoreProperties.txt")
set(DERIVED_CORE_PROP_PATH "${UNICODE_CACHE_LOCATION}/DerivedCoreProperties.txt")

set(DERIVED_BINARY_PROP_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/extracted/DerivedBinaryProperties.txt")
set(DERIVED_BINARY_PROP_PATH "${UNICODE_CACHE_LOCATION}/DerivedBinaryProperties.txt")

set(PROP_ALIAS_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/PropertyAliases.txt")
set(PROP_ALIAS_PATH "${UNICODE_CACHE_LOCATION}/PropertyAliases.txt")

set(PROP_VALUE_ALIAS_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/PropertyValueAliases.txt")
set(PROP_VALUE_ALIAS_PATH "${UNICODE_CACHE_LOCATION}/PropertyValueAliases.txt")

set(SCRIPTS_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/Scripts.txt")
set(SCRIPTS_PATH "${UNICODE_CACHE_LOCATION}/Scripts.txt")

set(SCRIPT_EXTENSIONS_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/ScriptExtensions.txt")
set(SCRIPT_EXTENSIONS_PATH "${UNICODE_CACHE_LOCATION}/ScriptExtensions.txt")

set(EMOJI_DATA_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/emoji/emoji-data.txt")
set(EMOJI_DATA_PATH "${UNICODE_CACHE_LOCATION}/emoji-data.txt")

set(NORM_PROPS_URL "https://www.unicode.org/Public/${UCD_VERSION}/ucd/DerivedNormalizationProps.txt")
set(NORM_PROPS_PATH "${UNICODE_CACHE_LOCATION}/DerivedNormalizationProps.txt")

set(CLDR_ZIP_URL "https://github.com/unicode-org/cldr-json/releases/download/${CLDR_VERSION}/cldr-${CLDR_VERSION}-json-modern.zip")
set(CLDR_ZIP_PATH "${LOCALE_DATA_CACHE_LOCATION}/cldr.zip")

set(CLDR_CORE_SOURCE cldr-core)
set(CLDR_CORE_PATH "${LOCALE_DATA_CACHE_LOCATION}/${CLDR_CORE_SOURCE}")

set(CLDR_LOCALES_SOURCE cldr-localenames-modern)
set(CLDR_LOCALES_PATH "${LOCALE_DATA_CACHE_LOCATION}/${CLDR_LOCALES_SOURCE}")

set(CLDR_MISC_SOURCE cldr-misc-modern)
set(CLDR_MISC_PATH "${LOCALE_DATA_CACHE_LOCATION}/${CLDR_MISC_SOURCE}")

set(CLDR_NUMBERS_SOURCE cldr-numbers-modern)
set(CLDR_NUMBERS_PATH "${LOCALE_DATA_CACHE_LOCATION}/${CLDR_NUMBERS_SOURCE}")

if (ENABLE_UNICODE_DATABASE_DOWNLOAD)
    if (NOT EXISTS ${UNICODE_DATA_PATH})
        message(STATUS "Downloading UCD UnicodeData.txt from ${UNICODE_DATA_URL}...")
        file(DOWNLOAD ${UNICODE_DATA_URL} ${UNICODE_DATA_PATH} INACTIVITY_TIMEOUT 10)
    endif()
    if (NOT EXISTS ${SPECIAL_CASING_PATH})
        message(STATUS "Downloading UCD SpecialCasing.txt from ${SPECIAL_CASING_URL}...")
        file(DOWNLOAD ${SPECIAL_CASING_URL} ${SPECIAL_CASING_PATH} INACTIVITY_TIMEOUT 10)
    endif()
    if (NOT EXISTS ${DERIVED_GENERAL_CATEGORY_PATH})
        message(STATUS "Downloading UCD DerivedGeneralCategory.txt from ${DERIVED_GENERAL_CATEGORY_URL}...")
        file(DOWNLOAD ${DERIVED_GENERAL_CATEGORY_URL} ${DERIVED_GENERAL_CATEGORY_PATH} INACTIVITY_TIMEOUT 10)
    endif()
    if (NOT EXISTS ${PROP_LIST_PATH})
        message(STATUS "Downloading UCD PropList.txt from ${PROP_LIST_URL}...")
        file(DOWNLOAD ${PROP_LIST_URL} ${PROP_LIST_PATH} INACTIVITY_TIMEOUT 10)
    endif()
    if (NOT EXISTS ${DERIVED_CORE_PROP_PATH})
        message(STATUS "Downloading UCD DerivedCoreProperties.txt from ${DERIVED_CORE_PROP_URL}...")
        file(DOWNLOAD ${DERIVED_CORE_PROP_URL} ${DERIVED_CORE_PROP_PATH} INACTIVITY_TIMEOUT 10)
    endif()
    if (NOT EXISTS ${DERIVED_BINARY_PROP_PATH})
        message(STATUS "Downloading UCD DerivedBinaryProperties.txt from ${DERIVED_BINARY_PROP_URL}...")
        file(DOWNLOAD ${DERIVED_BINARY_PROP_URL} ${DERIVED_BINARY_PROP_PATH} INACTIVITY_TIMEOUT 10)
    endif()
    if (NOT EXISTS ${PROP_ALIAS_PATH})
        message(STATUS "Downloading UCD PropertyAliases.txt from ${PROP_ALIAS_URL}...")
        file(DOWNLOAD ${PROP_ALIAS_URL} ${PROP_ALIAS_PATH} INACTIVITY_TIMEOUT 10)
    endif()
    if (NOT EXISTS ${PROP_VALUE_ALIAS_PATH})
        message(STATUS "Downloading UCD PropertyValueAliases.txt from ${PROP_VALUE_ALIAS_URL}...")
        file(DOWNLOAD ${PROP_VALUE_ALIAS_URL} ${PROP_VALUE_ALIAS_PATH} INACTIVITY_TIMEOUT 10)
    endif()
    if (NOT EXISTS ${SCRIPTS_PATH})
        message(STATUS "Downloading UCD Scripts.txt from ${SCRIPTS_URL}...")
        file(DOWNLOAD ${SCRIPTS_URL} ${SCRIPTS_PATH} INACTIVITY_TIMEOUT 10)
    endif()
    if (NOT EXISTS ${SCRIPT_EXTENSIONS_PATH})
        message(STATUS "Downloading UCD ScriptExtensions.txt from ${SCRIPT_EXTENSIONS_URL}...")
        file(DOWNLOAD ${SCRIPT_EXTENSIONS_URL} ${SCRIPT_EXTENSIONS_PATH} INACTIVITY_TIMEOUT 10)
    endif()
    if (NOT EXISTS ${EMOJI_DATA_PATH})
        message(STATUS "Downloading UCD emoji-data.txt from ${EMOJI_DATA_URL}...")
        file(DOWNLOAD ${EMOJI_DATA_URL} ${EMOJI_DATA_PATH} INACTIVITY_TIMEOUT 10)
    endif()
    if (NOT EXISTS ${NORM_PROPS_PATH})
        message(STATUS "Downloading UCD DerivedNormalizationProps.txt from ${NORM_PROPS_URL}...")
        file(DOWNLOAD ${NORM_PROPS_URL} ${NORM_PROPS_PATH} INACTIVITY_TIMEOUT 10)
    endif()

    if (NOT EXISTS ${CLDR_ZIP_PATH})
        message(STATUS "Downloading CLDR database from ${CLDR_ZIP_URL}...")
        file(DOWNLOAD ${CLDR_ZIP_URL} ${CLDR_ZIP_PATH} INACTIVITY_TIMEOUT 10)
    endif()
    if(EXISTS ${CLDR_ZIP_PATH} AND NOT EXISTS ${CLDR_CORE_PATH})
        message(STATUS "Extracting CLDR ${CLDR_CORE_SOURCE} from ${CLDR_ZIP_PATH}...")
        execute_process(COMMAND unzip -q ${CLDR_ZIP_PATH} "${CLDR_CORE_SOURCE}/**" -d ${LOCALE_DATA_CACHE_LOCATION} RESULT_VARIABLE unzip_result)
        if (NOT unzip_result EQUAL 0)
            message(FATAL_ERROR "Failed to unzip ${CLDR_CORE_SOURCE} from ${CLDR_ZIP_PATH} with status ${unzip_result}")
        endif()
    endif()
    if(EXISTS ${CLDR_ZIP_PATH} AND NOT EXISTS ${CLDR_LOCALES_PATH})
        message(STATUS "Extracting CLDR ${CLDR_LOCALES_SOURCE} from ${CLDR_ZIP_PATH}...")
        execute_process(COMMAND unzip -q ${CLDR_ZIP_PATH} "${CLDR_LOCALES_SOURCE}/**" -d ${LOCALE_DATA_CACHE_LOCATION} RESULT_VARIABLE unzip_result)
        if (NOT unzip_result EQUAL 0)
            message(FATAL_ERROR "Failed to unzip ${CLDR_LOCALES_SOURCE} from ${CLDR_ZIP_PATH} with status ${unzip_result}")
        endif()
    endif()
    if(EXISTS ${CLDR_ZIP_PATH} AND NOT EXISTS ${CLDR_MISC_PATH})
        message(STATUS "Extracting CLDR ${CLDR_MISC_SOURCE} from ${CLDR_ZIP_PATH}...")
        execute_process(COMMAND unzip -q ${CLDR_ZIP_PATH} "${CLDR_MISC_SOURCE}/**" -d ${LOCALE_DATA_CACHE_LOCATION} RESULT_VARIABLE unzip_result)
        if (NOT unzip_result EQUAL 0)
            message(FATAL_ERROR "Failed to unzip ${CLDR_MISC_SOURCE} from ${CLDR_ZIP_PATH} with status ${unzip_result}")
        endif()
    endif()
    if(EXISTS ${CLDR_ZIP_PATH} AND NOT EXISTS ${CLDR_NUMBERS_PATH})
        message(STATUS "Extracting CLDR ${CLDR_NUMBERS_SOURCE} from ${CLDR_ZIP_PATH}...")
        execute_process(COMMAND unzip -q ${CLDR_ZIP_PATH} "${CLDR_NUMBERS_SOURCE}/**" -d ${LOCALE_DATA_CACHE_LOCATION} RESULT_VARIABLE unzip_result)
        if (NOT unzip_result EQUAL 0)
            message(FATAL_ERROR "Failed to unzip ${CLDR_NUMBERS_SOURCE} from ${CLDR_ZIP_PATH} with status ${unzip_result}")
        endif()
    endif()

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
