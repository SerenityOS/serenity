#
# Builds the jakt bootstrap compiler as a host tool for Lagom to compile files written in jakt
#

include(FetchContent)

FetchContent_Declare(
  Corrosion
  GIT_REPOSITORY https://github.com/corrosion-rs/corrosion.git
  GIT_TAG v0.2.1
)

FetchContent_MakeAvailable(Corrosion)

FetchContent_Declare(jakt
    GIT_REPOSITORY https://github.com/SerenityOS/jakt.git
    GIT_TAG main
    GIT_SHALLOW TRUE
)

# Allow developers to skip download/update steps with local checkout
if (JAKT_SOURCE_DIR)
    set(FETCHCONTENT_SOURCE_DIR_JAKT ${JAKT_SOURCE_DIR} CACHE PATH "Developer's pre-existing jakt source directory" FORCE)
    message(STATUS "Using pre-existing JAKT_SOURCE_DIR: ${JAKT_SOURCE_DIR}")
endif()

FetchContent_GetProperties(jakt)
if (NOT jakt_POPULATED)
    FetchContent_Populate(jakt)
    corrosion_import_crate(MANIFEST_PATH "${jakt_SOURCE_DIR}/Cargo.toml")
    corrosion_set_hostbuild(jakt)
    add_executable(Lagom::jakt ALIAS jakt)
    corrosion_install(TARGETS jakt RUNTIME COMPONENT Lagom_Runtime)
    # NOTE: See lagom-install-config.cmake for hax required to get Lagom::jakt to show up on install
    install(DIRECTORY "${jakt_SOURCE_DIR}/runtime"
            DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}/jakt
            FILES_MATCHING PATTERN "*.h"
                           PATTERN "*.cpp"
                           PATTERN "utility")
endif()
