# Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
#
# SPDX-License-Identifier: BSD-2-Clause
#

#
# Source directory for androiddeployqt to use when bundling the application
#
set_property(TARGET ladybird APPEND PROPERTY
    QT_ANDROID_PACKAGE_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/android
)

#
# Android-specific sources and libs
#
add_library(android_init STATIC AndroidPlatform.cpp)
target_link_libraries(android_init PUBLIC Qt::Core Qt::Gui Qt::Network LibCompress LibArchive LibFileSystem)

macro(link_android_libs target)
    target_link_libraries(${target} PRIVATE android_init)
endmacro()

#
# NDK and Qt don't ship OpenSSL for Android
# Download the prebuilt binaries from KDAB for inclusion as recommended in Qt docs.
#
include(FetchContent)
FetchContent_Declare(android_openssl
    GIT_REPOSITORY https://github.com/KDAB/android_openssl
    GIT_TAG origin/master
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(android_openssl)
link_android_libs(ladybird)
target_link_libraries(ladybird PRIVATE Qt::Network)
set_property(TARGET ladybird APPEND PROPERTY QT_ANDROID_EXTRA_LIBS ${ANDROID_EXTRA_LIBS}
  "${CMAKE_CURRENT_BINARY_DIR}/WebContent/libWebContent_${ANDROID_ABI}.so"
  "${CMAKE_CURRENT_BINARY_DIR}/SQLServer/libSQLServer_${ANDROID_ABI}.so"
  "${CMAKE_CURRENT_BINARY_DIR}/WebDriver/libWebDriver_${ANDROID_ABI}.so"
)

#
# Copy resources into tarball for inclusion in /assets of APK
#
set(LADYBIRD_RESOURCE_ROOT "${SERENITY_SOURCE_DIR}/Base/res")
macro(copy_res_folder folder)
    add_custom_target(copy-${folder}
        COMMAND ${CMAKE_COMMAND} -E copy_directory
            "${LADYBIRD_RESOURCE_ROOT}/${folder}"
            "asset-bundle/res/${folder}"
    )
    add_dependencies(archive-assets copy-${folder})
endmacro()
add_custom_target(archive-assets COMMAND ${CMAKE_COMMAND} -E chdir asset-bundle tar czf ../ladybird-assets.tar.gz ./ )
copy_res_folder(html)
copy_res_folder(fonts)
copy_res_folder(icons)
copy_res_folder(emoji)
copy_res_folder(themes)
copy_res_folder(color-palettes)
copy_res_folder(cursor-themes)
add_custom_target(copy-autoplay-allowlist
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        "${SERENITY_SOURCE_DIR}/Base/home/anon/.config/BrowserAutoplayAllowlist.txt"
        "asset-bundle/res/ladybird/BrowserAutoplayAllowlist.txt"
)
add_custom_target(copy-content-filters
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        "${SERENITY_SOURCE_DIR}/Base/home/anon/.config/BrowserContentFilters.txt"
        "asset-bundle/res/ladybird/BrowserContentFilters.txt"
)
add_dependencies(archive-assets copy-autoplay-allowlist copy-content-filters)
add_custom_target(copy-assets COMMAND ${CMAKE_COMMAND} -E copy_if_different ladybird-assets.tar.gz "${CMAKE_SOURCE_DIR}/android/assets/")
add_dependencies(copy-assets archive-assets)
add_dependencies(ladybird copy-assets)
