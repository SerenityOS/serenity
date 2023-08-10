include(${CMAKE_CURRENT_LIST_DIR}/utils.cmake)

set(TZDB_PATH "${SERENITY_CACHE_DIR}/TZDB" CACHE PATH "Download location for TZDB files")

set(TZDB_VERSION 2023c)
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
    if(EXISTS "${TZDB_ZIP_PATH}" AND NOT EXISTS "${path}")
        if (CMAKE_VERSION VERSION_LESS 3.18.0)
            message(STATUS "Extracting using ${TAR_TOOL} file ${source}")
            execute_process(COMMAND "${TAR_TOOL}" -C "${TZDB_PATH}" -xzf "${TZDB_ZIP_PATH}" "${source}" RESULT_VARIABLE tar_result)
            if (NOT tar_result EQUAL 0)
                message(FATAL_ERROR "Failed to unzip ${source} from ${TZDB_ZIP_PATH} with status ${tar_result}")
            endif()
        else()
            message(STATUS "Extracting using cmake ${source}")
            file(ARCHIVE_EXTRACT INPUT "${TZDB_ZIP_PATH}" DESTINATION "${TZDB_PATH}" PATTERNS "${source}")
        endif()
    endif()
endfunction()

if (ENABLE_TIME_ZONE_DATABASE_DOWNLOAD)
    remove_path_if_version_changed("${TZDB_VERSION}" "${TZDB_VERSION_FILE}" "${TZDB_PATH}")

    if (ENABLE_NETWORK_DOWNLOADS)
        download_file("${TZDB_ZIP_URL}" "${TZDB_ZIP_PATH}")
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
