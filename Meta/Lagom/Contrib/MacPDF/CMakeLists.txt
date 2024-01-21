# This has the effect of making LC_RPATH absolute.
# Since the binary is in  Build/lagom/bin/MacPDF.app/Contents/MacOS/MacPDF,
# the default "@executable_path/../lib" doesn't work to get from the binary
# to Build/lagom/lib.
# FIXME: Pass "-Wl,-rpath,@executable_path/../../../../lib" instead for a relative path?
# Long-term, probably want to copy the dylibs into the bundle instead.
set(CMAKE_SKIP_BUILD_RPATH FALSE)
set(CMAKE_BUILD_WITH_INSTALL_RPATH FALSE)

add_compile_options(-DAK_DONT_REPLACE_STD)

set(RESOURCES
    MainMenu.xib
)

add_executable(MacPDF MACOSX_BUNDLE
    main.mm
    AppDelegate.mm
    MacPDFDocument.mm
    MacPDFOutlineViewDataSource.mm
    MacPDFView.mm
    MacPDFWindowController.mm
)
target_compile_options(MacPDF PRIVATE
    -fobjc-arc
)
target_link_libraries(MacPDF PRIVATE AK LibCore LibGfx LibPDF)
target_link_libraries(MacPDF PRIVATE
    "-framework Cocoa"
    "-framework UniformTypeIdentifiers"
)

set_target_properties(MacPDF PROPERTIES
    MACOSX_BUNDLE TRUE

    # FIXME: Apparently the Info.plist is only copied when the binary relinks,
    #        not if only the Info.plist contents changes and you rebuild?
    MACOSX_BUNDLE_INFO_PLIST "${CMAKE_CURRENT_SOURCE_DIR}/Info.plist"
)

# Normally you'd set `RESOURCE "${RESOURCES}"` on the MacPDF target properties
# and add `"${RESOURCES}" to the sources in add_executable()
# and CMake would add build steps to compile the xib files to nib files and
# add them to the bundle.
# But with CMake's ninja generator that seems to not work, so do it manually.
# See also https://github.com/dolphin-emu/dolphin/blob/2e39c79984490e/Source/Core/MacUpdater/CMakeLists.txt#L49-L56
find_program(IBTOOL ibtool HINTS "/usr/bin" "${OSX_DEVELOPER_ROOT}/usr/bin")
foreach(xib ${RESOURCES})
    string(REGEX REPLACE "[.]xib$" ".nib" nib "${xib}")

    # FIXME: This is gross! It makes the link at least as slow as compiling all xib files.
    # Better to have a separate command for the compiles and to only do the copying in the postbuild.
    add_custom_command(TARGET MacPDF POST_BUILD
        COMMAND ${IBTOOL} --errors --warnings --notices --output-format human-readable-text
                --compile "$<TARGET_BUNDLE_DIR:MacPDF>/Contents/Resources/${nib}"
                "${CMAKE_CURRENT_SOURCE_DIR}/${xib}"
        COMMENT "Compiling ${CMAKE_CURRENT_SOURCE_DIR}/${xib}.xib")
endforeach()
