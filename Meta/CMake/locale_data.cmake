include(${CMAKE_CURRENT_LIST_DIR}/utils.cmake)

set(CLDR_VERSION "45.0.0")
set(CLDR_SHA256 "ba934cdd40ad4fb6439004c7e746bef97fe2b597db1040fcaa6c7d0647742c1b")

set(CLDR_PATH "${SERENITY_CACHE_DIR}/CLDR" CACHE PATH "Download location for CLDR files")
set(CLDR_VERSION_FILE "${CLDR_PATH}/version.txt")

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

if (ENABLE_UNICODE_DATABASE_DOWNLOAD)
    remove_path_if_version_changed("${CLDR_VERSION}" "${CLDR_VERSION_FILE}" "${CLDR_PATH}")

    if (ENABLE_NETWORK_DOWNLOADS)
        download_file("${CLDR_ZIP_URL}" "${CLDR_ZIP_PATH}" SHA256 "${CLDR_SHA256}")
        extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_BCP47_SOURCE}/**" "${CLDR_BCP47_PATH}")
        extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_CORE_SOURCE}/**" "${CLDR_CORE_PATH}")
        extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_DATES_SOURCE}/**" "${CLDR_DATES_PATH}")
        extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_LOCALES_SOURCE}/**" "${CLDR_LOCALES_PATH}")
        extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_MISC_SOURCE}/**" "${CLDR_MISC_PATH}")
        extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_NUMBERS_SOURCE}/**" "${CLDR_NUMBERS_PATH}")
        extract_path("${CLDR_PATH}" "${CLDR_ZIP_PATH}" "${CLDR_UNITS_SOURCE}/**" "${CLDR_UNITS_PATH}")
    else()
        message(STATUS "Skipping download of ${CLDR_ZIP_URL}, expecting the archive to have been extracted to ${CLDR_PATH}")
    endif()

    set(DATE_TIME_FORMAT_DATA_HEADER DateTimeFormatData.h)
    set(DATE_TIME_FORMAT_DATA_IMPLEMENTATION DateTimeFormatData.cpp)

    set(LOCALE_DATA_HEADER LocaleData.h)
    set(LOCALE_DATA_IMPLEMENTATION LocaleData.cpp)

    set(NUMBER_FORMAT_DATA_HEADER NumberFormatData.h)
    set(NUMBER_FORMAT_DATA_IMPLEMENTATION NumberFormatData.cpp)

    set(PLURAL_RULES_DATA_HEADER PluralRulesData.h)
    set(PLURAL_RULES_DATA_IMPLEMENTATION PluralRulesData.cpp)

    set(RELATIVE_TIME_FORMAT_DATA_HEADER RelativeTimeFormatData.h)
    set(RELATIVE_TIME_FORMAT_DATA_IMPLEMENTATION RelativeTimeFormatData.cpp)

    invoke_generator(
        "DateTimeFormatData"
        Lagom::GenerateDateTimeFormatData
        "${CLDR_VERSION_FILE}"
        "${DATE_TIME_FORMAT_DATA_HEADER}"
        "${DATE_TIME_FORMAT_DATA_IMPLEMENTATION}"
        arguments -r "${CLDR_CORE_PATH}" -d "${CLDR_DATES_PATH}"
    )
    invoke_generator(
        "LocaleData"
        Lagom::GenerateLocaleData
        "${CLDR_VERSION_FILE}"
        "${LOCALE_DATA_HEADER}"
        "${LOCALE_DATA_IMPLEMENTATION}"
        arguments -b "${CLDR_BCP47_PATH}" -r "${CLDR_CORE_PATH}" -l "${CLDR_LOCALES_PATH}" -m "${CLDR_MISC_PATH}" -n "${CLDR_NUMBERS_PATH}" -d "${CLDR_DATES_PATH}"
    )
    invoke_generator(
        "NumberFormatData"
        Lagom::GenerateNumberFormatData
        "${CLDR_VERSION_FILE}"
        "${NUMBER_FORMAT_DATA_HEADER}"
        "${NUMBER_FORMAT_DATA_IMPLEMENTATION}"
        arguments -r "${CLDR_CORE_PATH}" -n "${CLDR_NUMBERS_PATH}" -u "${CLDR_UNITS_PATH}"
    )
    invoke_generator(
        "PluralRulesData"
        Lagom::GeneratePluralRulesData
        "${CLDR_VERSION_FILE}"
        "${PLURAL_RULES_DATA_HEADER}"
        "${PLURAL_RULES_DATA_IMPLEMENTATION}"
        arguments -r "${CLDR_CORE_PATH}" -l "${CLDR_LOCALES_PATH}"
    )
    invoke_generator(
        "RelativeTimeFormatData"
        Lagom::GenerateRelativeTimeFormatData
        "${CLDR_VERSION_FILE}"
        "${RELATIVE_TIME_FORMAT_DATA_HEADER}"
        "${RELATIVE_TIME_FORMAT_DATA_IMPLEMENTATION}"
        arguments -d "${CLDR_DATES_PATH}"
    )

    set(LOCALE_DATA_SOURCES
        ${DATE_TIME_FORMAT_DATA_HEADER}
        ${DATE_TIME_FORMAT_DATA_IMPLEMENTATION}
        ${LOCALE_DATA_HEADER}
        ${LOCALE_DATA_IMPLEMENTATION}
        ${NUMBER_FORMAT_DATA_HEADER}
        ${NUMBER_FORMAT_DATA_IMPLEMENTATION}
        ${PLURAL_RULES_DATA_HEADER}
        ${PLURAL_RULES_DATA_IMPLEMENTATION}
        ${RELATIVE_TIME_FORMAT_DATA_HEADER}
        ${RELATIVE_TIME_FORMAT_DATA_IMPLEMENTATION}
    )
endif()
