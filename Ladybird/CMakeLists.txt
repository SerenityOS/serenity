cmake_minimum_required(VERSION 3.16...3.22)

project(ladybird
    VERSION 0.0.1
    LANGUAGES CXX
    DESCRIPTION "Ladybird Web Browser"
)

include(GNUInstallDirs)

if (ANDROID)
    set(BUILD_SHARED_LIBS OFF)
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(CMAKE_SKIP_BUILD_RPATH FALSE)
set(CMAKE_BUILD_WITH_INSTALL_RPATH FALSE)
# See slide 100 of the following ppt :^)
# https://crascit.com/wp-content/uploads/2019/09/Deep-CMake-For-Library-Authors-Craig-Scott-CppCon-2019.pdf
if (NOT APPLE)
    set(CMAKE_INSTALL_RPATH $ORIGIN;$ORIGIN/../${CMAKE_INSTALL_LIBDIR})
endif()
set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

include(cmake/EnableLLD.cmake)

if (ENABLE_ADDRESS_SANITIZER)
    add_compile_options(-fsanitize=address -fno-omit-frame-pointer)
    add_link_options(-fsanitize=address)
endif()

if (ENABLE_MEMORY_SANITIZER)
    add_compile_options(-fsanitize=memory -fsanitize-memory-track-origins -fno-omit-frame-pointer)
    add_link_options(-fsanitize=memory -fsanitize-memory-track-origins)
endif()

if (ENABLE_UNDEFINED_SANITIZER)
    add_compile_options(-fsanitize=undefined -fno-omit-frame-pointer)
    add_link_options(-fsanitize=undefined)
endif()

# Lagom
# FIXME: PROJECT_IS_TOP_LEVEL with CMake 3.21+
set(LADYBIRD_IS_TOP_LEVEL FALSE)
set(LADYBIRD_CUSTOM_TARGET_SUFFIX "-ladybird")
if ("${CMAKE_BINARY_DIR}" STREQUAL "${PROJECT_BINARY_DIR}")
    set(LADYBIRD_IS_TOP_LEVEL TRUE)
    set(LADYBIRD_CUSTOM_TARGET_SUFFIX "")
endif()

if (LADYBIRD_IS_TOP_LEVEL)
    get_filename_component(
        SERENITY_SOURCE_DIR "${ladybird_SOURCE_DIR}/.."
        ABSOLUTE
    )
    list(APPEND CMAKE_MODULE_PATH "${SERENITY_SOURCE_DIR}/Meta/CMake")
    include(cmake/EnableLagom.cmake)
    include(lagom_compile_options NO_POLICY_SCOPE)
else()
    # FIXME: Use SERENITY_SOURCE_DIR in Lagom/CMakeLists.txt
    set(SERENITY_SOURCE_DIR "${SERENITY_PROJECT_ROOT}")
endif()

add_compile_options(-DAK_DONT_REPLACE_STD)
add_compile_options(-Wno-expansion-to-defined)
add_compile_options(-Wno-user-defined-literals)

set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_AUTOUIC ON)
find_package(Qt6 REQUIRED COMPONENTS Core Widgets Network Multimedia)

set(BROWSER_SOURCE_DIR ${SERENITY_SOURCE_DIR}/Userland/Applications/Browser/)

set(SOURCES
    ${BROWSER_SOURCE_DIR}/CookieJar.cpp
    ${BROWSER_SOURCE_DIR}/Database.cpp
    ${BROWSER_SOURCE_DIR}/History.cpp
    BrowserWindow.cpp
    ConsoleWidget.cpp
    EventLoopImplementationQt.cpp
    HelperProcess.cpp
    InspectorWidget.cpp
    LocationEdit.cpp
    ModelTranslator.cpp
    Settings.cpp
    SettingsDialog.cpp
    Tab.cpp
    TVGIconEngine.cpp
    Utilities.cpp
    WebContentView.cpp
    ladybird.qrc
    main.cpp
)

qt_add_executable(ladybird ${SOURCES})
target_link_libraries(ladybird PRIVATE Qt::Core Qt::Gui Qt::Network Qt::Widgets LibCore LibFileSystem LibGfx LibGUI LibIPC LibJS LibMain LibWeb LibWebView LibSQL LibProtocol)

target_include_directories(ladybird PRIVATE ${CMAKE_CURRENT_BINARY_DIR})
target_include_directories(ladybird PRIVATE ${SERENITY_SOURCE_DIR}/Userland/)
target_include_directories(ladybird PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Applications/)
target_include_directories(ladybird PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Services/)

qt_add_executable(headless-browser
    ${SERENITY_SOURCE_DIR}/Userland/Utilities/headless-browser.cpp
    ${SERENITY_SOURCE_DIR}/Userland/Services/WebContent/WebDriverConnection.cpp
    HelperProcess.cpp
    Utilities.cpp)

target_include_directories(headless-browser PRIVATE ${CMAKE_CURRENT_BINARY_DIR})
target_link_libraries(headless-browser PRIVATE Qt::Core LibWeb LibWebView LibWebSocket LibCrypto LibFileSystem LibGemini LibHTTP LibJS LibGfx LibMain LibTLS LibIPC LibDiff LibProtocol)

set_target_properties(ladybird PROPERTIES
    MACOSX_BUNDLE_GUI_IDENTIFIER org.SerenityOS.Ladybird
    MACOSX_BUNDLE_BUNDLE_VERSION ${PROJECT_VERSION}
    MACOSX_BUNDLE_SHORT_VERSION_STRING ${PROJECT_VERSION_MAJOR}.${PROJECT_VERSION_MINOR}
    MACOSX_BUNDLE_INFO_PLIST "${CMAKE_CURRENT_SOURCE_DIR}/Info.plist"
    MACOSX_BUNDLE TRUE
    WIN32_EXECUTABLE TRUE
    XCODE_ATTRIBUTE_PRODUCT_BUNDLE_IDENTIFIER org.SerenityOS.Ladybird
)

if (ANDROID)
    include(cmake/AndroidExtras.cmake)
    link_android_libs(headless-browser)
endif()

add_custom_target(run${LADYBIRD_CUSTOM_TARGET_SUFFIX}
    COMMAND "${CMAKE_COMMAND}" -E env "SERENITY_SOURCE_DIR=${SERENITY_SOURCE_DIR}" "$<TARGET_FILE:ladybird>" $ENV{LAGOM_ARGS}
    USES_TERMINAL
    VERBATIM
)

add_custom_target(debug${LADYBIRD_CUSTOM_TARGET_SUFFIX}
    COMMAND "${CMAKE_COMMAND}" -E env "SERENITY_SOURCE_DIR=${SERENITY_SOURCE_DIR}" gdb -ex "set follow-fork-mode child" "$<TARGET_FILE:ladybird>"
    USES_TERMINAL
)

add_subdirectory(SQLServer)
add_subdirectory(WebContent)
add_subdirectory(WebDriver)
add_subdirectory(RequestServer)
add_dependencies(ladybird SQLServer WebContent WebDriver RequestServer headless-browser)

if (APPLE)
    # FIXME: Create a proper app bundle for each helper process
    set(app_dir "$<TARGET_FILE_DIR:ladybird>")
    set(bundle_dir "$<TARGET_BUNDLE_DIR:ladybird>")
    add_custom_command(TARGET ladybird POST_BUILD
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different "$<TARGET_FILE:RequestServer>" "${app_dir}"
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different "$<TARGET_FILE:SQLServer>" "${app_dir}"
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different "$<TARGET_FILE:WebContent>" "${app_dir}"
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different "$<TARGET_FILE:WebDriver>" "${app_dir}"
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different "$<TARGET_FILE:headless-browser>" "${app_dir}"
    )
endif()

if(NOT CMAKE_SKIP_INSTALL_RULES)
    include(cmake/InstallRules.cmake)
endif()

include(CTest)
if (BUILD_TESTING)
    add_test(
        NAME LibWeb
        COMMAND ${CMAKE_CURRENT_BINARY_DIR}/../bin/headless-browser --run-tests ${SERENITY_SOURCE_DIR}/Tests/LibWeb
    )
    set_tests_properties(LibWeb PROPERTIES ENVIRONMENT QT_QPA_PLATFORM=offscreen)
endif()
