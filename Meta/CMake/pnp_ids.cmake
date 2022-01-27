include(${CMAKE_CURRENT_LIST_DIR}/utils.cmake)

set(PNP_IDS_FILE pnp.ids)
set(PNP_IDS_URL http://www.uefi.org/uefi-pnp-export)
set(PNP_IDS_EXPORT_PATH ${CMAKE_BINARY_DIR}/pnp_ids.html)
set(PNP_IDS_INSTALL_PATH ${CMAKE_INSTALL_DATAROOTDIR}/${PNP_IDS_FILE})

if (ENABLE_PNP_IDS_DOWNLOAD)
    file(MAKE_DIRECTORY ${CMAKE_INSTALL_DATAROOTDIR})
    download_file("${PNP_IDS_URL}" "${PNP_IDS_EXPORT_PATH}")

    set(PNP_IDS_HEADER PnpIDs.h)
    set(PNP_IDS_IMPLEMENTATION PnpIDs.cpp)
    set(PNP_IDS_TARGET_PREFIX "")

    invoke_generator(
        "PnpIDsData"
        Lagom::GeneratePnpIDsData
        "${PNP_IDS_EXPORT_PATH}"
        "${PNP_IDS_TARGET_PREFIX}"
        "${PNP_IDS_HEADER}"
        "${PNP_IDS_IMPLEMENTATION}"
        arguments -p "${PNP_IDS_EXPORT_PATH}"
    )

    set(PNP_IDS_SOURCES
        ${PNP_IDS_HEADER}
        ${PNP_IDS_IMPLEMENTATION}
    )
endif()
