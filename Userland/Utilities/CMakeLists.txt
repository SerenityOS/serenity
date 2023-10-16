set(CMD_SOURCES_CPP
    abench.cpp
    aconv.cpp
    adjtime.cpp
    allocate.cpp
    animation.cpp
    aplay.cpp
    arp.cpp
    asctl.cpp
    base64.cpp
    basename.cpp
    beep.cpp
    blockdev.cpp
    bt.cpp
    cal.cpp
    cat.cpp
    checksum.cpp
    chgrp.cpp
    chmod.cpp
    chown.cpp
    chres.cpp
    cksum.cpp
    clear.cpp
    cmp.cpp
    comm.cpp
    config.cpp
    copy.cpp
    copy_mount.cpp
    cp.cpp
    cpp-lexer.cpp
    cpp-parser.cpp
    cpp-preprocessor.cpp
    crypto-bench.cpp
    cut.cpp
    date.cpp
    dd.cpp
    df.cpp
    diff.cpp
    dirname.cpp
    disasm.cpp
    disk_benchmark.cpp
    dmesg.cpp
    drain.cpp
    du.cpp
    echo.cpp
    elfdeps.cpp
    env.cpp
    errno.cpp
    expr.cpp
    false.cpp
    fdtdump.cpp
    file.cpp
    find.cpp
    flock.cpp
    fortune.cpp
    functrace.cpp
    fusermount.cpp
    glsl-compiler.cpp
    gml-format.cpp
    grep.cpp
    gron.cpp
    groupadd.cpp
    groupdel.cpp
    groups.cpp
    gzip.cpp
    head.cpp
    headless-browser.cpp
    hexdump.cpp
    hiddump.cpp
    host.cpp
    hostname.cpp
    icc.cpp
    iconv.cpp
    id.cpp
    ifconfig.cpp
    image.cpp
    image2bin.cpp
    imgcmp.cpp
    ini.cpp
    init.cpp
    install.cpp
    isobmff.cpp
    js.cpp
    json.cpp
    kcov-example.cpp
    keymap.cpp
    kill.cpp
    killall.cpp
    less.cpp
    listdir.cpp
    ln.cpp
    logout.cpp
    ls.cpp
    lsblk.cpp
    lscpu.cpp
    lsdev.cpp
    lsirq.cpp
    lsof.cpp
    lspci.cpp
    lsusb.cpp
    lzcat.cpp
    man.cpp
    markdown-check.cpp
    matroska.cpp
    md.cpp
    memstat.cpp
    mkdir.cpp
    mkfifo.cpp
    mkfs.fat.cpp
    mknod.cpp
    mktemp.cpp
    mount.cpp
    mv.cpp
    nc.cpp
    netstat.cpp
    network-settings.cpp
    nl.cpp
    nohup.cpp
    nologin.cpp
    notify.cpp
    nproc.cpp
    ntpquery.cpp
    open.cpp
    passwd.cpp
    paste.cpp
    patch.cpp
    pathchk.cpp
    pdf.cpp
    pgrep.cpp
    pidof.cpp
    ping.cpp
    pixelflut.cpp
    pkill.cpp
    pledge.cpp
    pls.cpp
    pmap.cpp
    pmemdump.cpp
    printf.cpp
    pro.cpp
    profile.cpp
    ps.cpp
    purge.cpp
    pwd.cpp
    readelf.cpp
    readlink.cpp
    realpath.cpp
    reboot.cpp
    rev.cpp
    rm.cpp
    rmdir.cpp
    route.cpp
    run-tests.cpp
    sed.cpp
    seq.cpp
    shot.cpp
    shred.cpp
    shuf.cpp
    shutdown.cpp
    sizefmt.cpp
    sleep.cpp
    slugify.cpp
    sort.cpp
    sql.cpp
    stat.cpp
    strace.cpp
    strings.cpp
    stty.cpp
    su.cpp
    sync.cpp
    syscall.cpp
    sysctl.cpp
    tac.cpp
    tail.cpp
    tar.cpp
    tee.cpp
    telws.cpp
    test-bindtodevice.cpp
    test-fuzz.cpp
    test-imap.cpp
    test-jpeg-roundtrip.cpp
    test-pthread.cpp
    test-unveil.cpp
    test.cpp
    test_env.cpp
    timezone.cpp
    top.cpp
    touch.cpp
    tr.cpp
    traceroute.cpp
    tree.cpp
    true.cpp
    truncate.cpp
    tsort.cpp
    tt.cpp
    ttfdisasm.cpp
    tty.cpp
    umount.cpp
    uname.cpp
    uniq.cpp
    unveil.cpp
    unzip.cpp
    update-cpp-test-results.cpp
    uptime.cpp
    useradd.cpp
    userdel.cpp
    usermod.cpp
    utmpupdate.cpp
    w.cpp
    wallpaper.cpp
    wasm.cpp
    watch.cpp
    watchfs.cpp
    wc.cpp
    which.cpp
    whoami.cpp
    wsctl.cpp
    xargs.cpp
    xml.cpp
    xxd.cpp
    xzcat.cpp
    yes.cpp
    zip.cpp
)
set(CMD_SOURCES_JAKT
    hello-world.jakt
)
if (ENABLE_JAKT)
    list(APPEND CMD_SOURCES ${CMD_SOURCES_CPP} ${CMD_SOURCES_JAKT})
else()
    list(APPEND CMD_SOURCES ${CMD_SOURCES_CPP})
endif()

list(APPEND SPECIAL_TARGETS test install)
list(APPEND REQUIRED_TARGETS
    arp base64 basename cat chmod chown clear comm cp cut date dd df diff dirname dmesg du echo env expr false
    file find grep groups head host hostname id ifconfig init kill killall ln logout ls mkdir mount mv network-settings nproc
    patch pgrep pidof ping pkill pmap ps readlink realpath reboot rm rmdir sed route seq shutdown sleep sort stat stty su tail test
    touch tr true umount uname uniq uptime w watchfs wc which whoami xargs yes
)
list(APPEND RECOMMENDED_TARGETS
    aconv adjtime aplay abench asctl bt checksum chres cksum copy fortune gzip install keymap lsdev lsirq lsof lspci lzcat man mkfs.fat mknod mktemp
    nc netstat notify ntpquery open passwd pixelflut pls printf pro shot strings tar tt unzip wallpaper xzcat zip
)

# FIXME: Support specifying component dependencies for utilities (e.g. WebSocket for telws)

foreach(CMD_SRC ${CMD_SOURCES})
    get_filename_component(CMD_NAME ${CMD_SRC} NAME_WLE)
    get_filename_component(CMD_EXT ${CMD_SRC} LAST_EXT)
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

    serenity_add_executable(${TARGET_NAME} ${CMD_SRC})
    serenity_set_implicit_links(${TARGET_NAME})
    set_target_properties(${TARGET_NAME} PROPERTIES EXCLUDE_FROM_ALL TRUE)
    target_link_libraries(${TARGET_NAME} PRIVATE LibCore)
    if(NOT ${CMD_EXT} EQUAL jakt)
        target_link_libraries(${TARGET_NAME} PRIVATE LibMain)
    endif()

    install(TARGETS ${TARGET_NAME} RUNTIME DESTINATION bin OPTIONAL)

    if (CMD_NAME IN_LIST SPECIAL_TARGETS)
        install(CODE
            "if (EXISTS ${CMAKE_INSTALL_PREFIX}/bin/${CMD_NAME}-bin)
                file(RENAME ${CMAKE_INSTALL_PREFIX}/bin/${CMD_NAME}-bin ${CMAKE_INSTALL_PREFIX}/bin/${CMD_NAME})
            endif()")
    endif()
endforeach()

install(CODE "file(CREATE_LINK grep ${CMAKE_INSTALL_PREFIX}/bin/egrep SYMBOLIC)")
install(CODE "file(CREATE_LINK grep ${CMAKE_INSTALL_PREFIX}/bin/fgrep SYMBOLIC)")
install(CODE "file(CREATE_LINK grep ${CMAKE_INSTALL_PREFIX}/bin/rgrep SYMBOLIC)")
install(CODE "file(CREATE_LINK gzip ${CMAKE_INSTALL_PREFIX}/bin/gunzip SYMBOLIC)")
install(CODE "file(CREATE_LINK gzip ${CMAKE_INSTALL_PREFIX}/bin/zcat SYMBOLIC)")
install(CODE "file(CREATE_LINK /usr/lib/Loader.so ${CMAKE_INSTALL_PREFIX}/bin/ldd SYMBOLIC)")

target_link_libraries(abench PRIVATE LibAudio LibFileSystem)
target_link_libraries(aconv PRIVATE LibAudio LibFileSystem)
target_link_libraries(animation PRIVATE LibGfx)
target_link_libraries(aplay PRIVATE LibAudio LibFileSystem LibIPC)
target_link_libraries(asctl PRIVATE LibAudio LibIPC)
target_link_libraries(bt PRIVATE LibSymbolication LibURL)
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
target_link_libraries(crypto-bench PRIVATE LibCrypto)
target_link_libraries(diff PRIVATE LibDiff)
target_link_libraries(disasm PRIVATE LibELF LibDisassembly)
target_link_libraries(drain PRIVATE LibFileSystem)
target_link_libraries(elfdeps PRIVATE LibELF)
target_link_libraries(expr PRIVATE LibRegex)
target_link_libraries(fdtdump PRIVATE LibDeviceTree)
target_link_libraries(file PRIVATE LibELF LibGfx LibIPC LibArchive LibCompress LibAudio)
target_link_libraries(find PRIVATE LibFileSystem LibRegex LibURL)
target_link_libraries(functrace PRIVATE LibDebug LibELF LibDisassembly)
target_link_libraries(glsl-compiler PRIVATE LibGLSL)
target_link_libraries(gml-format PRIVATE LibGUI)
target_link_libraries(grep PRIVATE LibFileSystem LibRegex LibURL)
target_link_libraries(gzip PRIVATE LibCompress)
target_link_libraries(headless-browser PRIVATE LibCrypto LibFileSystem LibGemini LibGfx LibHTTP LibImageDecoderClient LibTLS LibWeb LibWebView LibWebSocket LibIPC LibJS LibDiff LibURL)
target_link_libraries(hiddump PRIVATE LibHID)
target_link_libraries(icc PRIVATE LibGfx LibMedia LibURL)
target_link_libraries(iconv PRIVATE LibTextCodec)
target_link_libraries(image PRIVATE LibGfx)
target_link_libraries(image2bin PRIVATE LibGfx)
target_link_libraries(imgcmp PRIVATE LibGfx)
target_link_libraries(ini PRIVATE LibFileSystem)
target_link_libraries(install-bin PRIVATE LibFileSystem)
target_link_libraries(isobmff PRIVATE LibGfx)
target_link_libraries(js PRIVATE LibCrypto LibJS LibLine LibLocale LibTextCodec)
target_link_libraries(keymap PRIVATE LibKeyboard)
target_link_libraries(less PRIVATE LibLine)
target_link_libraries(ls PRIVATE LibFileSystem LibURL)
target_link_libraries(lspci PRIVATE LibPCIDB)
target_link_libraries(lsusb PRIVATE LibUSBDB)
target_link_libraries(lzcat PRIVATE LibCompress)
target_link_libraries(man PRIVATE LibMarkdown LibManual)
target_link_libraries(markdown-check PRIVATE LibFileSystem LibMarkdown LibManual LibURL)
target_link_libraries(matroska PRIVATE LibMedia)
target_link_libraries(md PRIVATE LibMarkdown)
target_link_libraries(mkfs.fat PRIVATE LibFileSystem)
target_link_libraries(mktemp PRIVATE LibFileSystem)
target_link_libraries(mv PRIVATE LibFileSystem)
target_link_libraries(notify PRIVATE LibGfx LibGUI LibURL)
target_link_libraries(open PRIVATE LibDesktop LibFileSystem LibURL)
target_link_libraries(passwd PRIVATE LibCrypt)
target_link_libraries(paste PRIVATE LibGUI)
target_link_libraries(patch PRIVATE LibDiff LibFileSystem)
target_link_libraries(pdf PRIVATE LibGfx LibPDF)
target_link_libraries(pgrep PRIVATE LibRegex)
target_link_libraries(pixelflut PRIVATE LibImageDecoderClient LibIPC LibGfx)
target_link_libraries(pkill PRIVATE LibRegex)
target_link_libraries(pledge PRIVATE LibELF)
target_link_libraries(pls PRIVATE LibCrypt)
target_link_libraries(pro PRIVATE LibFileSystem LibProtocol LibHTTP LibURL)
target_link_libraries(readelf PRIVATE LibELF)
target_link_libraries(readlink PRIVATE LibFileSystem)
target_link_libraries(realpath PRIVATE LibFileSystem)
target_link_libraries(run-tests PRIVATE LibCoredump LibDebug LibELF LibFileSystem LibRegex)
target_link_libraries(rm PRIVATE LibFileSystem)
target_link_libraries(sed PRIVATE LibRegex LibFileSystem)
target_link_libraries(shot PRIVATE LibFileSystem LibGfx LibGUI LibIPC LibURL)
target_link_libraries(shred PRIVATE LibFileSystem)
target_link_libraries(slugify PRIVATE LibUnicode)
target_link_libraries(sql PRIVATE LibFileSystem LibIPC LibLine LibSQL)
target_link_libraries(su PRIVATE LibCrypt)
target_link_libraries(syscall PRIVATE LibSystem)
target_link_libraries(ttfdisasm PRIVATE LibGfx)
target_link_libraries(tar PRIVATE LibArchive LibCompress LibFileSystem)
target_link_libraries(telws PRIVATE LibProtocol LibLine LibURL)
target_link_libraries(test-imap PRIVATE LibIMAP)
target_link_libraries(test-jpeg-roundtrip PRIVATE LibGfx)
target_link_libraries(test-pthread PRIVATE LibThreading)
target_link_libraries(touch PRIVATE LibFileSystem)
target_link_libraries(unzip PRIVATE LibArchive LibCompress LibCrypto LibFileSystem)
target_link_libraries(update-cpp-test-results PRIVATE LibCpp)
target_link_libraries(useradd PRIVATE LibCrypt)
target_link_libraries(userdel PRIVATE LibFileSystem)
target_link_libraries(usermod PRIVATE LibFileSystem)
target_link_libraries(wallpaper PRIVATE LibGfx LibGUI)
target_link_libraries(wasm PRIVATE LibFileSystem LibJS LibLine LibWasm)
target_link_libraries(watch PRIVATE LibFileSystem)
target_link_libraries(wsctl PRIVATE LibGUI LibIPC)
target_link_libraries(xml PRIVATE LibFileSystem LibXML LibURL)
target_link_libraries(xxd PRIVATE LibUnicode)
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
add_subdirectory(runc)
