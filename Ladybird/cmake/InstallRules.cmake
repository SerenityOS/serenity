
include(CMakePackageConfigHelpers)
include(GNUInstallDirs)

set(package ladybird)

set(ladybird_applications ladybird SQLServer WebContent WebDriver WebSocketServer RequestServer headless-browser)

set(app_install_targets ${ladybird_applications})

install(TARGETS ${app_install_targets}
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
  FILE_SET browser
    DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
  FILE_SET ladybird
    DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
)

include("${SERENITY_SOURCE_DIR}/Meta/Lagom/get_linked_lagom_libraries.cmake")
foreach (application IN LISTS ladybird_applications)
  get_linked_lagom_libraries("${application}" "${application}_lagom_libraries")
  list(APPEND all_required_lagom_libraries "${${application}_lagom_libraries}")
endforeach()
list(REMOVE_DUPLICATES all_required_lagom_libraries)

# Remove ladybird shlib if it exists
list(REMOVE_ITEM all_required_lagom_libraries ladybird)

# Install service impl libraries if they exist
macro(install_service_lib service)
    if (TARGET ${service})
      get_target_property(target_type ${service} TYPE)
      if ("${target_type}" STREQUAL STATIC_LIBRARY)
          list(APPEND all_required_lagom_libraries ${service})
      endif()
    endif()
endmacro()
foreach(service IN LISTS webcontent requestserver websocket)
    install_service_lib(${service})
endforeach()

install(TARGETS ${all_required_lagom_libraries}
  EXPORT ladybirdTargets
  COMPONENT ladybird_Runtime
  LIBRARY
    COMPONENT ladybird_Runtime
    NAMELINK_COMPONENT ladybird_Development
    DESTINATION ${CMAKE_INSTALL_LIBDIR}
  FILE_SET server
    DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
  FILE_SET ladybird
    DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
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
    "${SERENITY_SOURCE_DIR}/Base/home/anon/.config/BrowserAutoplayAllowlist.txt"
    "${SERENITY_SOURCE_DIR}/Base/home/anon/.config/BrowserContentFilters.txt"
    "${Lagom_BINARY_DIR}/cacert.pem"
  DESTINATION "${CMAKE_INSTALL_DATADIR}/res/ladybird"
  COMPONENT ladybird_Runtime
)

if (APPLE)
  # Fixup the app bundle and copy:
  #   - Libraries from lib/ to ladybird.app/Contents/lib
  #   - Resources from share/res/ to ladybird.app/Contents/Resources/res
  install(CODE "
    set(res_dir \${CMAKE_INSTALL_PREFIX}/${CMAKE_INSTALL_DATADIR}/res)
    if (IS_ABSOLUTE ${CMAKE_INSTALL_DATADIR})
      set(res_dir ${CMAKE_INSTALL_DATADIR}/res)
    endif()
    set(lib_dir \${CMAKE_INSTALL_PREFIX}/${CMAKE_INSTALL_LIBDIR})
    if (IS_ABSOLUTE ${CMAKE_INSTALL_LIBDIR})
      set(lib_dir ${CMAKE_INSTALL_LIBDIR})
    endif()

    set(contents_dir \${CMAKE_INSTALL_PREFIX}/bundle/ladybird.app/Contents)
    file(COPY \${res_dir} DESTINATION \${contents_dir}/Resources)
    file(COPY \${lib_dir} DESTINATION \${contents_dir})
  "
  COMPONENT ladybird_Runtime)
endif()
