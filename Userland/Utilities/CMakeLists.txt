file(GLOB CMD_SOURCES  CONFIGURE_DEPENDS "*.cpp")
list(APPEND SPECIAL_TARGETS test install)
list(APPEND REQUIRED_TARGETS
    arp base64 basename cat chmod chown clear comm cp cut date dd df dirname dmesg du echo env expr false fgrep
    file find grep groups head host hostname id ifconfig kill killall ln ls mkdir mount mv nproc
    pgrep pidof ping pmap ps readlink realpath reboot rm rmdir seq shutdown sleep sort stat stty su tail test
    touch tr true umount uname uniq uptime w wc which whoami xargs yes less
)
list(APPEND RECOMMENDED_TARGETS
    adjtime aplay avol bt checksum chres cksum copy fortune gunzip gzip init keymap lsirq lsof lspci man mediaprobe 
    mknod mktemp modload modunload nc netstat notify ntpquery open pape passwd pls printf pro shot tar tt unzip zip
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
    if (CMD_NAME IN_LIST SPECIAL_TARGETS)
        add_executable(${TARGET_NAME} ${CMD_SRC})
        target_link_libraries(${TARGET_NAME} LibCore)
        install(TARGETS ${TARGET_NAME} RUNTIME DESTINATION bin)
        install(CODE "file(RENAME ${CMAKE_INSTALL_PREFIX}/bin/${CMD_NAME}-bin ${CMAKE_INSTALL_PREFIX}/bin/${CMD_NAME})")
    else()
        add_executable(${CMD_NAME} ${CMD_SRC})
        set_target_properties(${CMD_NAME} PROPERTIES EXCLUDE_FROM_ALL TRUE)
        target_link_libraries(${CMD_NAME} LibCore)
        install(TARGETS ${CMD_NAME} RUNTIME DESTINATION bin OPTIONAL)
    endif()
endforeach()

target_link_libraries(aplay LibAudio)
target_link_libraries(avol LibAudio)
target_link_libraries(bt LibSymbolication)
target_link_libraries(checksum LibCrypto)
target_link_libraries(chres LibGUI)
target_link_libraries(cksum LibCrypto)
target_link_libraries(copy LibGUI)
target_link_libraries(crash LibTest)
target_link_libraries(disasm LibX86)
target_link_libraries(expr LibRegex)
target_link_libraries(file LibGfx LibIPC LibCompress)
target_link_libraries(functrace LibDebug LibX86)
target_link_libraries(gml-format LibGUI)
target_link_libraries(grep LibRegex)
target_link_libraries(gunzip LibCompress)
target_link_libraries(gzip LibCompress)
target_link_libraries(js LibJS LibLine)
target_link_libraries(keymap LibKeyboard)
target_link_libraries(lspci LibPCIDB)
target_link_libraries(lsusb LibUSBDB)
target_link_libraries(man LibMarkdown)
target_link_libraries(matroska LibVideo)
target_link_libraries(md LibMarkdown)
target_link_libraries(misbehaving-application LibCore)
target_link_libraries(notify LibGUI)
target_link_libraries(open LibDesktop)
target_link_libraries(pape LibGUI)
target_link_libraries(passwd LibCrypt)
target_link_libraries(paste LibGUI)
target_link_libraries(pgrep LibRegex)
target_link_libraries(pls LibCrypt)
target_link_libraries(pro LibProtocol)
target_link_libraries(run-tests LibRegex)
target_link_libraries(shot LibGUI)
target_link_libraries(sql LibLine LibSQL LibIPC)
target_link_libraries(su LibCrypt)
target_link_libraries(tar LibArchive LibCompress)
target_link_libraries(telws LibProtocol LibLine)
target_link_libraries(test-crypto LibCrypto LibTLS LibLine)
target_link_libraries(test-fuzz LibCore LibGemini LibGfx LibHTTP LibIPC LibJS LibMarkdown LibShell)
target_link_libraries(test-imap LibIMAP)
target_link_libraries(test-pthread LibThreading)
target_link_libraries(tt LibPthread)
target_link_libraries(unzip LibArchive LibCompress)
target_link_libraries(zip LibArchive LibCompress LibCrypto)
target_link_libraries(cpp-parser LibCpp LibGUI)
target_link_libraries(cpp-preprocessor LibCpp LibGUI)
target_link_libraries(wasm LibWasm LibLine)
target_link_libraries(wsctl LibGUI)
target_link_libraries(mediaprobe LibMedia)
