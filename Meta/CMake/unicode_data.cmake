option(ENABLE_UNICODE_DATABASE_DOWNLOAD "Enable download of Unicode UCD files at build time" ON)

set(UNICODE_DATA_URL https://www.unicode.org/Public/13.0.0/ucd/UnicodeData.txt)
set(UNICODE_DATA_PATH ${CMAKE_BINARY_DIR}/UCD/UnicodeData.txt)

set(SPECIAL_CASING_URL https://www.unicode.org/Public/13.0.0/ucd/SpecialCasing.txt)
set(SPECIAL_CASING_PATH ${CMAKE_BINARY_DIR}/UCD/SpecialCasing.txt)

set(DERIVED_GENERAL_CATEGORY_URL https://www.unicode.org/Public/13.0.0/ucd/extracted/DerivedGeneralCategory.txt)
set(DERIVED_GENERAL_CATEGORY_PATH ${CMAKE_BINARY_DIR}/UCD/DerivedGeneralCategory.txt)

set(PROP_LIST_URL https://www.unicode.org/Public/13.0.0/ucd/PropList.txt)
set(PROP_LIST_PATH ${CMAKE_BINARY_DIR}/UCD/PropList.txt)

set(DERIVED_CORE_PROP_URL https://www.unicode.org/Public/13.0.0/ucd/DerivedCoreProperties.txt)
set(DERIVED_CORE_PROP_PATH ${CMAKE_BINARY_DIR}/UCD/DerivedCoreProperties.txt)

set(DERIVED_BINARY_PROP_URL https://www.unicode.org/Public/13.0.0/ucd/extracted/DerivedBinaryProperties.txt)
set(DERIVED_BINARY_PROP_PATH ${CMAKE_BINARY_DIR}/UCD/DerivedBinaryProperties.txt)

set(PROP_ALIAS_URL https://www.unicode.org/Public/13.0.0/ucd/PropertyAliases.txt)
set(PROP_ALIAS_PATH ${CMAKE_BINARY_DIR}/UCD/PropertyAliases.txt)

set(PROP_VALUE_ALIAS_URL https://www.unicode.org/Public/13.0.0/ucd/PropertyValueAliases.txt)
set(PROP_VALUE_ALIAS_PATH ${CMAKE_BINARY_DIR}/UCD/PropertyValueAliases.txt)

set(SCRIPTS_URL https://www.unicode.org/Public/13.0.0/ucd/Scripts.txt)
set(SCRIPTS_PATH ${CMAKE_BINARY_DIR}/UCD/Scripts.txt)

set(SCRIPT_EXTENSIONS_URL https://www.unicode.org/Public/13.0.0/ucd/ScriptExtensions.txt)
set(SCRIPT_EXTENSIONS_PATH ${CMAKE_BINARY_DIR}/UCD/ScriptExtensions.txt)

set(EMOJI_DATA_URL https://www.unicode.org/Public/13.0.0/ucd/emoji/emoji-data.txt)
set(EMOJI_DATA_PATH ${CMAKE_BINARY_DIR}/UCD/emoji-data.txt)

set(NORM_PROPS_URL https://www.unicode.org/Public/13.0.0/ucd/DerivedNormalizationProps.txt)
set(NORM_PROPS_PATH ${CMAKE_BINARY_DIR}/UCD/DerivedNormalizationProps.txt)

set(CLDR_PATH ${CMAKE_BINARY_DIR}/CLDR)
set(CLDR_ZIP_URL https://github.com/unicode-org/cldr-json/releases/download/39.0.0/cldr-39.0.0-json-modern.zip)
set(CLDR_ZIP_PATH ${CLDR_PATH}/cldr.zip)

set(CLDR_LOCALES_SOURCE cldr-localenames-modern)
set(CLDR_LOCALES_PATH ${CLDR_PATH}/${CLDR_LOCALES_SOURCE})

set(CLDR_NUMBERS_SOURCE cldr-numbers-modern)
set(CLDR_NUMBERS_PATH ${CLDR_PATH}/${CLDR_NUMBERS_SOURCE})

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
    if(EXISTS ${CLDR_ZIP_PATH} AND NOT EXISTS ${CLDR_LOCALES_PATH})
        message(STATUS "Extracting CLDR ${CLDR_LOCALES_SOURCE} from ${CLDR_ZIP_PATH}...")
        execute_process(COMMAND unzip -q ${CLDR_ZIP_PATH} "${CLDR_LOCALES_SOURCE}/**" -d ${CLDR_PATH} RESULT_VARIABLE unzip_result)
        if (NOT unzip_result EQUAL 0)
            message(FATAL_ERROR "Failed to unzip ${CLDR_LOCALES_SOURCE} from ${CLDR_ZIP_PATH} with status ${unzip_result}")
        endif()
    endif()
    if(EXISTS ${CLDR_ZIP_PATH} AND NOT EXISTS ${CLDR_NUMBERS_PATH})
        message(STATUS "Extracting CLDR ${CLDR_NUMBERS_SOURCE} from ${CLDR_ZIP_PATH}...")
        execute_process(COMMAND unzip -q ${CLDR_ZIP_PATH} "${CLDR_NUMBERS_SOURCE}/**" -d ${CLDR_PATH} RESULT_VARIABLE unzip_result)
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
        COMMAND $<TARGET_FILE:GenerateUnicodeData> -h ${UNICODE_DATA_HEADER} -c ${UNICODE_DATA_IMPLEMENTATION} -u ${UNICODE_DATA_PATH} -s ${SPECIAL_CASING_PATH} -g ${DERIVED_GENERAL_CATEGORY_PATH} -p ${PROP_LIST_PATH} -d ${DERIVED_CORE_PROP_PATH} -b ${DERIVED_BINARY_PROP_PATH} -a ${PROP_ALIAS_PATH} -v ${PROP_VALUE_ALIAS_PATH} -r ${SCRIPTS_PATH} -x ${SCRIPT_EXTENSIONS_PATH} -e ${EMOJI_DATA_PATH} -n ${NORM_PROPS_PATH}
        VERBATIM
        DEPENDS GenerateUnicodeData ${UNICODE_DATA_PATH} ${SPECIAL_CASING_PATH} ${DERIVED_GENERAL_CATEGORY_PATH} ${PROP_LIST_PATH} ${DERIVED_CORE_PROP_PATH} ${DERIVED_BINARY_PROP_PATH} ${PROP_ALIAS_PATH} ${PROP_VALUE_ALIAS_PATH} ${SCRIPTS_PATH} ${SCRIPT_EXTENSIONS_PATH} ${EMOJI_DATA_PATH} ${NORM_PROPS_PATH}
    )
    add_custom_target(generate_${UNICODE_META_TARGET_PREFIX}UnicodeData DEPENDS ${UNICODE_DATA_HEADER} ${UNICODE_DATA_IMPLEMENTATION})
    add_dependencies(all_generated generate_${UNICODE_META_TARGET_PREFIX}UnicodeData)

    add_custom_command(
        OUTPUT ${UNICODE_LOCALE_HEADER} ${UNICODE_LOCALE_IMPLEMENTATION}
        COMMAND $<TARGET_FILE:GenerateUnicodeLocale> -h ${UNICODE_LOCALE_HEADER} -c ${UNICODE_LOCALE_IMPLEMENTATION} -l ${CLDR_LOCALES_PATH} -n ${CLDR_NUMBERS_PATH}
        VERBATIM
        DEPENDS GenerateUnicodeLocale ${CLDR_LOCALES_PATH} ${CLDR_NUMBERS_PATH}
    )
    add_custom_target(generate_${UNICODE_META_TARGET_PREFIX}UnicodeLocale DEPENDS ${UNICODE_LOCALE_HEADER} ${UNICODE_LOCALE_IMPLEMENTATION})
    add_dependencies(all_generated generate_${UNICODE_META_TARGET_PREFIX}UnicodeLocale)

    set(UNICODE_DATA_SOURCES ${UNICODE_DATA_HEADER} ${UNICODE_DATA_IMPLEMENTATION} ${UNICODE_LOCALE_HEADER} ${UNICODE_LOCALE_IMPLEMENTATION})
endif()
