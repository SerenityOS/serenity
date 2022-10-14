#
# Check for the dependencies that the Serenity (target) and Lagom (host) builds require.
#

# FIXME: With cmake 3.18, we can change unzip/untar/gzip steps to use
#        file(  ARCHIVE_EXTRACT) instead
#
#        Additionally we have to emit an error message for each tool,
#        as REQUIRED only works with cmake 3.18 and above.
if (CMAKE_VERSION VERSION_LESS 3.18.0)
    find_program(UNZIP_TOOL unzip REQUIRED)
    message(STATUS "Found cmake ${CMAKE_VERSION} - testing for external tools to uncompress")
    if (NOT UNZIP_TOOL)
        message(FATAL_ERROR "Failed to locate unzip on your machine, please install it and re-read the SerenityOS build documentation.")
    endif()

    find_program(TAR_TOOL tar REQUIRED)
    if (NOT TAR_TOOL)
        message(FATAL_ERROR "Failed to locate tar on your machine, please install it and re-read the SerenityOS build documentation.")
    endif()

    find_program(GZIP_TOOL gzip REQUIRED)
    if (NOT GZIP_TOOL)
        message(FATAL_ERROR "Failed to locate gzip on your machine, please install it and re-read the SerenityOS build documentation.")
    endif()
endif()
