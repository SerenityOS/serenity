include(${CMAKE_CURRENT_LIST_DIR}/utils.cmake)

set(TZDB_VERSION "2024a")
set(TZDB_SHA256 "0d0434459acbd2059a7a8da1f3304a84a86591f6ed69c6248fffa502b6edffe3")

set(TZDB_PATH "${SERENITY_CACHE_DIR}/TZDB" CACHE PATH "Download location for TZDB files")
set(TZDB_VERSION_FILE "${TZDB_PATH}/version.txt")

set(TZDB_ZIP_URL "https://data.iana.org/time-zones/releases/tzdata${TZDB_VERSION}.tar.gz")
set(TZDB_ZIP_PATH "${TZDB_PATH}/tzdb.tar.gz")

set(TZDB_AFRICA_SOURCE africa)
set(TZDB_AFRICA_PATH "${TZDB_PATH}/${TZDB_AFRICA_SOURCE}")

set(TZDB_ANTARCTICA_SOURCE antarctica)
set(TZDB_ANTARCTICA_PATH "${TZDB_PATH}/${TZDB_ANTARCTICA_SOURCE}")

set(TZDB_ASIA_SOURCE asia)
set(TZDB_ASIA_PATH "${TZDB_PATH}/${TZDB_ASIA_SOURCE}")

set(TZDB_AUSTRALASIA_SOURCE australasia)
set(TZDB_AUSTRALASIA_PATH "${TZDB_PATH}/${TZDB_AUSTRALASIA_SOURCE}")

set(TZDB_BACKWARD_SOURCE backward)
set(TZDB_BACKWARD_PATH "${TZDB_PATH}/${TZDB_BACKWARD_SOURCE}")

set(TZDB_ETCETERA_SOURCE etcetera)
set(TZDB_ETCETERA_PATH "${TZDB_PATH}/${TZDB_ETCETERA_SOURCE}")

set(TZDB_EUROPE_SOURCE europe)
set(TZDB_EUROPE_PATH "${TZDB_PATH}/${TZDB_EUROPE_SOURCE}")

set(TZDB_NORTH_AMERICA_SOURCE northamerica)
set(TZDB_NORTH_AMERICA_PATH "${TZDB_PATH}/${TZDB_NORTH_AMERICA_SOURCE}")

set(TZDB_SOUTH_AMERICA_SOURCE southamerica)
set(TZDB_SOUTH_AMERICA_PATH "${TZDB_PATH}/${TZDB_SOUTH_AMERICA_SOURCE}")

set(TZDB_ZONE_1970_SOURCE zone1970.tab)
set(TZDB_ZONE_1970_PATH "${TZDB_PATH}/${TZDB_ZONE_1970_SOURCE}")

function(extract_tzdb_file source path)
    extract_path("${TZDB_PATH}" "${TZDB_ZIP_PATH}" "${source}" "${path}")
endfunction()

if (ENABLE_TIME_ZONE_DATABASE_DOWNLOAD)
    if (ENABLE_NETWORK_DOWNLOADS)
        download_file("${TZDB_ZIP_URL}" "${TZDB_ZIP_PATH}" SHA256 "${TZDB_SHA256}" VERSION "${TZDB_VERSION}" VERSION_FILE "${TZDB_VERSION_FILE}" CACHE_PATH "${TZDB_PATH}")
        extract_tzdb_file("${TZDB_AFRICA_SOURCE}" "${TZDB_AFRICA_PATH}")
        extract_tzdb_file("${TZDB_ANTARCTICA_SOURCE}" "${TZDB_ANTARCTICA_PATH}")
        extract_tzdb_file("${TZDB_ASIA_SOURCE}" "${TZDB_ASIA_PATH}")
        extract_tzdb_file("${TZDB_AUSTRALASIA_SOURCE}" "${TZDB_AUSTRALASIA_PATH}")
        extract_tzdb_file("${TZDB_BACKWARD_SOURCE}" "${TZDB_BACKWARD_PATH}")
        extract_tzdb_file("${TZDB_ETCETERA_SOURCE}" "${TZDB_ETCETERA_PATH}")
        extract_tzdb_file("${TZDB_EUROPE_SOURCE}" "${TZDB_EUROPE_PATH}")
        extract_tzdb_file("${TZDB_NORTH_AMERICA_SOURCE}" "${TZDB_NORTH_AMERICA_PATH}")
        extract_tzdb_file("${TZDB_SOUTH_AMERICA_SOURCE}" "${TZDB_SOUTH_AMERICA_PATH}")
        extract_tzdb_file("${TZDB_ZONE_1970_SOURCE}" "${TZDB_ZONE_1970_PATH}")
    else()
        message(STATUS "Skipping download of ${TZDB_ZIP_URL}, expecting the archive to have been extracted to ${TZDB_PATH}")
    endif()

    invoke_generator(
        "TimeZoneData"
        Lagom::GenerateTimeZoneData
        "${TZDB_VERSION_FILE}"
        "TimeZoneData.h"
        "TimeZoneData.cpp"
        arguments -z "${TZDB_ZONE_1970_PATH}" "${TZDB_AFRICA_PATH}" "${TZDB_ANTARCTICA_PATH}" "${TZDB_ASIA_PATH}" "${TZDB_AUSTRALASIA_PATH}" "${TZDB_BACKWARD_PATH}" "${TZDB_ETCETERA_PATH}" "${TZDB_EUROPE_PATH}" "${TZDB_NORTH_AMERICA_PATH}" "${TZDB_SOUTH_AMERICA_PATH}"
    )

    set(TIME_ZONE_DATA_SOURCES
        "TimeZoneData.h"
        "TimeZoneData.cpp"
    )
endif()
