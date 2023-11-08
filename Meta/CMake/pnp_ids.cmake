include(${CMAKE_CURRENT_LIST_DIR}/utils.cmake)

set(PNP_IDS_URL https://uefi.org/uefi-pnp-export)
set(PNP_IDS_EXPORT_PATH ${SERENITY_CACHE_DIR}/pnp_ids.csv)

if (ENABLE_PNP_IDS_DOWNLOAD)
    download_file("${PNP_IDS_URL}" "${PNP_IDS_EXPORT_PATH}")

    set(PNP_IDS_HEADER PnpIDs.h)
    set(PNP_IDS_IMPLEMENTATION PnpIDs.cpp)

    invoke_generator(
        "PnpIDsData"
        Lagom::GeneratePnpIDsData
        "${PNP_IDS_EXPORT_PATH}"
        "${PNP_IDS_HEADER}"
        "${PNP_IDS_IMPLEMENTATION}"
        arguments -p "${PNP_IDS_EXPORT_PATH}"
    )

    set(PNP_IDS_SOURCES
        ${PNP_IDS_HEADER}
        ${PNP_IDS_IMPLEMENTATION}
    )
endif()
