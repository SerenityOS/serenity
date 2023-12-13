include(${CMAKE_CURRENT_LIST_DIR}/utils.cmake)

set(CACERT_PATH "${SERENITY_CACHE_DIR}/CACERT" CACHE PATH "Download location for cacert.pem")

set(CACERT_VERSION 2023-12-12)
set(CACERT_VERSION_FILE "${CACERT_PATH}/version.txt")

set(CACERT_FILE cacert-${CACERT_VERSION}.pem)
set(CACERT_URL https://curl.se/ca/${CACERT_FILE})
set(CACERT_INSTALL_FILE cacert.pem)

if (ENABLE_CACERT_DOWNLOAD)
    remove_path_if_version_changed("${CACERT_VERSION}" "${CACERT_VERSION_FILE}" "${CACERT_PATH}")

    download_file("${CACERT_URL}" "${CACERT_PATH}/${CACERT_FILE}")

    if (NOT "${CMAKE_STAGING_PREFIX}" STREQUAL "")
        set(CACERT_INSTALL_PATH ${CMAKE_STAGING_PREFIX}/etc/${CACERT_INSTALL_FILE})
    else()
        set(CACERT_INSTALL_PATH ${CMAKE_CURRENT_BINARY_DIR}/${CACERT_INSTALL_FILE})
    endif()
    configure_file(${CACERT_PATH}/${CACERT_FILE} ${CACERT_INSTALL_PATH} COPYONLY)
endif()
