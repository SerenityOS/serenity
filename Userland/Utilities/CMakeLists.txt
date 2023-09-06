file(GLOB CMD_SOURCES  CONFIGURE_DEPENDS "*.cpp")
list(APPEND SPECIAL_TARGETS test install)
list(APPEND REQUIRED_TARGETS
    arp base64 basename cat chmod chown clear comm cp cut date dd df diff dirname dmesg du echo env expr false
    file find grep groups gunzip head host hostname id ifconfig kill killall ln logout ls mkdir mount mv network-settings nproc
    patch pgrep pidof ping pkill pmap ps readlink realpath reboot rm rmdir sed route seq shutdown sleep sort stat stty su tail test
    touch tr true umount uname uniq uptime w wc which whoami xargs yes
)
list(APPEND RECOMMENDED_TARGETS
    aconv adjtime aplay abench asctl bt checksum chres cksum copy fortune gzip init install keymap lsirq lsof lspci lzcat man mknod mktemp
    nc netstat notify ntpquery open passwd pixelflut pls printf pro shot strings tar tt unzip wallpaper xzcat zip
)

# FIXME: Support specifying component dependencies for utilities (e.g. WebSocket for telws)

foreach(CMD_SRC ${CMD_SOURCES})
    get_filename_component(CMD_NAME ${CMD_SRC} NAME_WE)
    if (CMD_NAME IN_LIST SPECIAL_TARGETS)
        set(TARGET_NAME "${CMD_NAME}-bin")
    else()
        set(TARGET_NAME "${CMD_NAME}")
    endif()
    if(CMD_NAME IN_LIST REQUIRED_TARGETS)
        serenity_component(
            ${CMD_NAME}
            REQUIRED
            TARGETS ${TARGET_NAME}
        )
    elseif(CMD_NAME IN_LIST RECOMMENDED_TARGETS)
        serenity_component(
            ${CMD_NAME}
            RECOMMENDED
            TARGETS ${TARGET_NAME}
        )
    else()
        serenity_component(
            ${CMD_NAME}
            TARGETS ${TARGET_NAME}
        )
    endif()

    add_executable(${TARGET_NAME} ${CMD_SRC})
    target_link_libraries(${TARGET_NAME} PRIVATE LibCore LibMain)
    serenity_set_implicit_links(${TARGET_NAME})
    set_target_properties(${TARGET_NAME} PROPERTIES EXCLUDE_FROM_ALL TRUE)
    install(TARGETS ${TARGET_NAME} RUNTIME DESTINATION bin OPTIONAL)

    if (CMD_NAME IN_LIST SPECIAL_TARGETS)
        install(CODE
            "if (EXISTS ${CMAKE_INSTALL_PREFIX}/bin/${CMD_NAME}-bin)
                file(RENAME ${CMAKE_INSTALL_PREFIX}/bin/${CMD_NAME}-bin ${CMAKE_INSTALL_PREFIX}/bin/${CMD_NAME})
            endif()")
    endif()
endforeach()

if (ENABLE_JAKT)
    add_jakt_executable(hello-jakt
        MAIN_SOURCE hello-world.jakt
    )
    target_link_libraries(hello-jakt PRIVATE LibC)
    set_target_properties(hello-jakt PROPERTIES EXCLUDE_FROM_ALL TRUE)
    install(TARGETS hello-jakt RUNTIME DESTINATION bin OPTIONAL)
    serenity_component(
        hello-jakt
        RECOMMENDED
        TARGETS hello-jakt
    )
endif()

install(CODE "file(CREATE_LINK grep ${CMAKE_INSTALL_PREFIX}/bin/egrep SYMBOLIC)")
install(CODE "file(CREATE_LINK grep ${CMAKE_INSTALL_PREFIX}/bin/fgrep SYMBOLIC)")
install(CODE "file(CREATE_LINK grep ${CMAKE_INSTALL_PREFIX}/bin/rgrep SYMBOLIC)")
install(CODE "file(CREATE_LINK gunzip ${CMAKE_INSTALL_PREFIX}/bin/zcat SYMBOLIC)")

target_link_libraries(abench PRIVATE LibAudio LibFileSystem)
target_link_libraries(aconv PRIVATE LibAudio LibFileSystem)
target_link_libraries(aplay PRIVATE LibAudio LibFileSystem LibIPC)
target_link_libraries(asctl PRIVATE LibAudio LibIPC)
target_link_libraries(bt PRIVATE LibSymbolication)
target_link_libraries(checksum PRIVATE LibCrypto)
target_link_libraries(chres PRIVATE LibGUI LibIPC)
target_link_libraries(cksum PRIVATE LibCrypto)
target_link_libraries(config PRIVATE LibConfig LibIPC)
target_link_libraries(copy PRIVATE LibGUI)
target_link_libraries(comm PRIVATE LibFileSystem)
target_link_libraries(cp PRIVATE LibFileSystem)
target_link_libraries(cpp-lexer PRIVATE LibCpp)
target_link_libraries(cpp-parser PRIVATE LibCpp)
target_link_libraries(cpp-preprocessor PRIVATE LibCpp)
target_link_libraries(diff PRIVATE LibDiff)
target_link_libraries(disasm PRIVATE LibX86)
target_link_libraries(expr PRIVATE LibRegex)
target_link_libraries(fdtdump PRIVATE LibDeviceTree)
target_link_libraries(file PRIVATE LibGfx LibIPC LibArchive LibCompress LibAudio)
target_link_libraries(find PRIVATE LibFileSystem LibRegex)
target_link_libraries(functrace PRIVATE LibDebug LibX86)
target_link_libraries(glsl-compiler PRIVATE LibGLSL)
target_link_libraries(gml-format PRIVATE LibGUI)
target_link_libraries(grep PRIVATE LibFileSystem LibRegex)
target_link_libraries(gunzip PRIVATE LibCompress)
target_link_libraries(gzip PRIVATE LibCompress)
target_link_libraries(headless-browser PRIVATE LibCrypto LibFileSystem LibGemini LibGfx LibHTTP LibTLS LibWeb LibWebView LibWebSocket LibIPC LibJS LibDiff)
target_link_libraries(icc PRIVATE LibGfx LibVideo)
target_link_libraries(image PRIVATE LibGfx)
target_link_libraries(image2bin PRIVATE LibGfx)
target_link_libraries(ini PRIVATE LibFileSystem)
target_link_libraries(install-bin PRIVATE LibFileSystem)
target_link_libraries(isobmff PRIVATE LibGfx)
target_link_libraries(jail-attach PRIVATE LibCore LibMain)
target_link_libraries(jail-create PRIVATE LibCore LibMain)
target_link_libraries(js PRIVATE LibCrypto LibJS LibLine LibLocale LibTextCodec)
link_with_locale_data(js)
target_link_libraries(keymap PRIVATE LibKeyboard)
target_link_libraries(less PRIVATE LibLine)
target_link_libraries(ls PRIVATE LibFileSystem)
target_link_libraries(lspci PRIVATE LibPCIDB)
target_link_libraries(lsusb PRIVATE LibUSBDB)
target_link_libraries(lzcat PRIVATE LibCompress)
target_link_libraries(man PRIVATE LibMarkdown LibManual)
target_link_libraries(markdown-check PRIVATE LibFileSystem LibMarkdown LibManual)
target_link_libraries(matroska PRIVATE LibVideo)
target_link_libraries(md PRIVATE LibMarkdown)
target_link_libraries(mktemp PRIVATE LibFileSystem)
target_link_libraries(mv PRIVATE LibFileSystem)
target_link_libraries(network-settings PRIVATE LibCore LibMain)
target_link_libraries(notify PRIVATE LibGfx LibGUI)
target_link_libraries(open PRIVATE LibDesktop LibFileSystem)
target_link_libraries(passwd PRIVATE LibCrypt)
target_link_libraries(paste PRIVATE LibGUI)
target_link_libraries(patch PRIVATE LibDiff LibFileSystem)
target_link_libraries(pdf PRIVATE LibGfx LibPDF)
target_link_libraries(pgrep PRIVATE LibRegex)
target_link_libraries(pixelflut PRIVATE LibImageDecoderClient LibIPC LibGfx)
target_link_libraries(pkill PRIVATE LibRegex)
target_link_libraries(pls PRIVATE LibCrypt)
target_link_libraries(pro PRIVATE LibFileSystem LibProtocol LibHTTP)
target_link_libraries(readlink PRIVATE LibFileSystem)
target_link_libraries(run-tests PRIVATE LibCoredump LibDebug LibFileSystem LibRegex)
target_link_libraries(rm PRIVATE LibFileSystem)
target_link_libraries(sed PRIVATE LibRegex LibFileSystem)
target_link_libraries(shot PRIVATE LibFileSystem LibGfx LibGUI LibIPC)
target_link_libraries(sql PRIVATE LibFileSystem LibIPC LibLine LibSQL)
target_link_libraries(su PRIVATE LibCrypt)
target_link_libraries(syscall PRIVATE LibSystem)
target_link_libraries(ttfdisasm PRIVATE LibGfx)
target_link_libraries(tar PRIVATE LibArchive LibCompress LibFileSystem)
target_link_libraries(telws PRIVATE LibProtocol LibLine)
target_link_libraries(test-imap PRIVATE LibIMAP)
target_link_libraries(test-pthread PRIVATE LibThreading)
target_link_libraries(touch PRIVATE LibFileSystem)
target_link_libraries(unveil PRIVATE LibMain)
target_link_libraries(unzip PRIVATE LibArchive LibCompress LibCrypto LibFileSystem)
target_link_libraries(update-cpp-test-results PRIVATE LibCpp)
target_link_libraries(useradd PRIVATE LibCrypt)
target_link_libraries(userdel PRIVATE LibFileSystem)
target_link_libraries(usermod PRIVATE LibFileSystem)
target_link_libraries(wallpaper PRIVATE LibGfx LibGUI)
target_link_libraries(wasm PRIVATE LibFileSystem LibJS LibLine LibWasm)
target_link_libraries(watch PRIVATE LibFileSystem)
target_link_libraries(wsctl PRIVATE LibGUI LibIPC)
target_link_libraries(xml PRIVATE LibFileSystem LibXML)
target_link_libraries(xzcat PRIVATE LibCompress)
target_link_libraries(zip PRIVATE LibArchive LibFileSystem)

# FIXME: Link this file into headless-browser without compiling it again.
target_sources(headless-browser PRIVATE "${SerenityOS_SOURCE_DIR}/Userland/Services/WebContent/WebDriverConnection.cpp")

include("${SerenityOS_SOURCE_DIR}/Meta/Lagom/Fuzzers/fuzzers.cmake")

foreach(name IN LISTS FUZZER_TARGETS)
    set(fuzz_source_file "${SerenityOS_SOURCE_DIR}/Meta/Lagom/Fuzzers/Fuzz${name}.cpp")
    target_sources(test-fuzz PRIVATE "${fuzz_source_file}")
    set_source_files_properties("${fuzz_source_file}" PROPERTIES COMPILE_DEFINITIONS "LLVMFuzzerTestOneInput=Test${name}")
    set_source_files_properties("${fuzz_source_file}" PROPERTIES COMPILE_OPTIONS "-Wno-missing-declarations")
    target_link_libraries(test-fuzz PRIVATE "${FUZZER_DEPENDENCIES_${name}}")
endforeach()

add_subdirectory(pkg)
