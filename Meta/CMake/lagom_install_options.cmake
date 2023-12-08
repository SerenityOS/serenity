include_guard()

# Skip trying to setup install rules if no languages are enabled, such as in the Superbuild.
get_property(languages GLOBAL PROPERTY ENABLED_LANGUAGES)
if (NONE IN_LIST languages)
    return()
endif()

include(GNUInstallDirs) # make sure to include before we mess w/RPATH

# Mirror the structure of the installed tree to ensure that rpaths
# always remain valid.
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/${CMAKE_INSTALL_BINDIR}")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/${CMAKE_INSTALL_LIBDIR}")
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/${CMAKE_INSTALL_LIBDIR}")

set(CMAKE_SKIP_BUILD_RPATH FALSE)
set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
# See slide 100 of the following ppt :^)
# https://crascit.com/wp-content/uploads/2019/09/Deep-CMake-For-Library-Authors-Craig-Scott-CppCon-2019.pdf
if (APPLE)
    set(CMAKE_MACOSX_RPATH TRUE)
    set(CMAKE_INSTALL_NAME_DIR "@rpath")
    set(CMAKE_INSTALL_RPATH "@executable_path/../lib")
else()
    set(CMAKE_INSTALL_RPATH "$ORIGIN:$ORIGIN/../${CMAKE_INSTALL_LIBDIR}")
endif()
set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)

set(CMAKE_INSTALL_MESSAGE NEVER)
