#
# Builds the jakt bootstrap compiler as a host tool for Lagom to compile files written in jakt
#

include(FetchContent)

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

macro(add_jakt_compiler_flags target)
  target_compile_options("${target}" PRIVATE
    -Wno-unused-local-typedefs
    -Wno-unused-function
    -Wno-unknown-warning-option
    -Wno-trigraphs
    -Wno-parentheses-equality
    -Wno-unqualified-std-cast-call
    -Wno-user-defined-literals
    -Wno-deprecated-declarations
    -Wno-unused-variable
    -Wno-unused-result
    -Wno-unused-parameter
    -Wno-return-type
    -Wno-unused-but-set-variable
  )
  target_compile_features("${target}" PRIVATE cxx_std_20)
endmacro()

FetchContent_GetProperties(jakt)
if (NOT jakt_POPULATED)
    FetchContent_Populate(jakt)

    if (NOT JAKT_SOURCE_DIR)
        set(JAKT_SOURCE_DIR ${jakt_SOURCE_DIR})
    endif()

    add_executable(jakt ${JAKT_SOURCE_DIR}/bootstrap/stage0/jakt.cpp)

    add_jakt_compiler_flags(jakt)
    target_include_directories(jakt
      PUBLIC
        $<BUILD_INTERFACE:${JAKT_SOURCE_DIR}/bootstrap/stage0/runtime>
        $<INSTALL_INTERFACE:runtime>
    )
    
    # NOTE: See lagom-install-config.cmake for hax required to get Lagom::jakt to show up on install    
    install(
        TARGETS jakt
        EXPORT JaktTargets
        RUNTIME #
            DESTINATION "${CMAKE_INSTALL_BINDIR}"
            COMPONENT Jakt_Runtime
        LIBRARY #
            DESTINATION "${CMAKE_INSTALL_LIBDIR}/jakt"
            COMPONENT Jakt_Runtime
            NAMELINK_COMPONENT Jakt_Development
        ARCHIVE #
            DESTINATION "${CMAKE_INSTALL_LIBDIR}/jakt"
            COMPONENT Jakt_Development
    )

    install(DIRECTORY "${JAKT_SOURCE_DIR}/runtime"
            DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}/jakt
            FILES_MATCHING PATTERN "*.h"
                           PATTERN "*.cpp"
                           PATTERN "utility")

    install(
        EXPORT JaktTargets
        NAMESPACE Jakt::
        DESTINATION "${CMAKE_INSTALL_DATADIR}/jakt"
        COMPONENT Jakt_Development
    )
endif()

