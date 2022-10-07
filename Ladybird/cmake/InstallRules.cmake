
include(CMakePackageConfigHelpers)
include(GNUInstallDirs)

set(package ladybird)

install(TARGETS ladybird
  EXPORT ladybirdTargets
  RUNTIME
    COMPONENT ladybird_Runtime
    DESTINATION ${CMAKE_INSTALL_BINDIR}
  BUNDLE
    COMPONENT ladybird_Runtime
    DESTINATION bundle
  LIBRARY
    COMPONENT ladybird_Runtime
    NAMELINK_COMPONENT ladybird_Development
    DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

install(TARGETS WebContent
  EXPORT ladybirdTargets
  RUNTIME
    COMPONENT ladybird_Runtime
    DESTINATION ${CMAKE_INSTALL_BINDIR}
  BUNDLE
    COMPONENT ladybird_Runtime
    DESTINATION bundle
)

include("${Lagom_SOURCE_DIR}/get_linked_lagom_libraries.cmake")
get_linked_lagom_libraries(ladybird ladybird_lagom_libraries)
get_linked_lagom_libraries(WebContent webcontent_lagom_libraries)
list(APPEND all_required_lagom_libraries ${ladybird_lagom_libraries} ${webcontent_lagom_libraries})
list(REMOVE_DUPLICATES all_required_lagom_libraries)

install(TARGETS ${all_required_lagom_libraries}
  EXPORT ladybirdTargets
  COMPONENT ladybird_Runtime
  LIBRARY
    COMPONENT ladybird_Runtime
    NAMELINK_COMPONENT ladybird_Development
    DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

write_basic_package_version_file(
    "${package}ConfigVersion.cmake"
    COMPATIBILITY SameMajorVersion
)

# Allow package maintainers to freely override the path for the configs
set(
    ladybird_INSTALL_CMAKEDIR "${CMAKE_INSTALL_DATADIR}/${package}"
    CACHE PATH "CMake package config location relative to the install prefix"
)
mark_as_advanced(ladybird_INSTALL_CMAKEDIR)

install(
    FILES cmake/LadybirdInstallConfig.cmake
    DESTINATION "${ladybird_INSTALL_CMAKEDIR}"
    RENAME "${package}Config.cmake"
    COMPONENT ladybird_Development
)

install(
    FILES "${PROJECT_BINARY_DIR}/${package}ConfigVersion.cmake"
    DESTINATION "${ladybird_INSTALL_CMAKEDIR}"
    COMPONENT ladybird_Development
)

install(
    EXPORT ladybirdTargets
    NAMESPACE ladybird::
    DESTINATION "${ladybird_INSTALL_CMAKEDIR}"
    COMPONENT ladybird_Development
)

install(DIRECTORY
    "${SERENITY_SOURCE_DIR}/Base/res/html"
    "${SERENITY_SOURCE_DIR}/Base/res/fonts"
    "${SERENITY_SOURCE_DIR}/Base/res/icons"
    "${SERENITY_SOURCE_DIR}/Base/res/themes"
    "${SERENITY_SOURCE_DIR}/Base/res/color-palettes"
    "${SERENITY_SOURCE_DIR}/Base/res/cursor-themes"
  DESTINATION "${CMAKE_INSTALL_DATADIR}/res"
  USE_SOURCE_PERMISSIONS MESSAGE_NEVER
  COMPONENT ladybird_Runtime
)

install(FILES
    "${SERENITY_SOURCE_DIR}/Base/home/anon/.config/BrowserContentFilters.txt"
  DESTINATION "${CMAKE_INSTALL_DATADIR}/res/ladybird"
  COMPONENT ladybird_Runtime
)
