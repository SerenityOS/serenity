serenity_component(
    BuggieBox
    REQUIRED
    TARGETS BuggieBox
)

function (buggiebox_utility src)
    get_filename_component(utility ${src} NAME_WE)
    target_sources(LibBuggieBox PRIVATE ${src})
    set_source_files_properties(${src} PROPERTIES COMPILE_DEFINITIONS "serenity_main=${utility}_main")
endfunction()

set(utility_srcs
    ../Utilities/base64.cpp
    ../Utilities/blockdev.cpp
    ../Utilities/cat.cpp
    ../Utilities/clear.cpp
    ../Utilities/checksum.cpp
    ../Utilities/chmod.cpp
    ../Utilities/chown.cpp
    ../Utilities/cp.cpp
    ../Utilities/dd.cpp
    ../Utilities/df.cpp
    ../Utilities/dmesg.cpp
    ../Utilities/env.cpp
    ../Utilities/errno.cpp
    ../Utilities/file.cpp
    ../Utilities/find.cpp
    ../Utilities/head.cpp
    ../Utilities/id.cpp
    ../Utilities/less.cpp
    ../Utilities/ldd.cpp
    ../Utilities/ln.cpp
    ../Utilities/ls.cpp
    ../Utilities/lsblk.cpp
    ../Utilities/lscpu.cpp
    ../Utilities/lsirq.cpp
    ../Utilities/lspci.cpp
    ../Utilities/lsusb.cpp
    ../Utilities/mkdir.cpp
    ../Utilities/mknod.cpp
    ../Utilities/mktemp.cpp
    ../Utilities/mount.cpp
    ../Utilities/mv.cpp
    ../Utilities/pledge.cpp
    ../Utilities/ps.cpp
    ../Utilities/readelf.cpp
    ../Utilities/rm.cpp
    ../Utilities/rmdir.cpp
    ../Utilities/shuf.cpp
    ../Utilities/sort.cpp
    ../Utilities/stat.cpp
    ../Utilities/sync.cpp
    ../Utilities/sysctl.cpp
    ../Utilities/tac.cpp
    ../Utilities/tail.cpp
    ../Utilities/test.cpp
    ../Utilities/timezone.cpp
    ../Utilities/top.cpp
    ../Utilities/touch.cpp
    ../Utilities/tree.cpp
    ../Utilities/umount.cpp
    ../Utilities/uname.cpp
    ../Utilities/uniq.cpp
    ../Utilities/unveil.cpp
    ../Utilities/whoami.cpp
)

serenity_lib(LibBuggieBox buggiebox)
target_compile_definitions(LibBuggieBox PRIVATE -DEXCLUDE_SERENITY_MAIN)
target_link_libraries(LibBuggieBox PRIVATE LibShellStatic LibGfxStatic LibCoreStatic LibIPCStatic LibRegexStatic LibCompressStatic LibCryptoStatic LibLineStatic LibUSBDBStatic LibPCIDBStatic)
target_sources(LibBuggieBox PRIVATE ../Shell/main.cpp)
set_source_files_properties( ../Shell/main.cpp PROPERTIES COMPILE_DEFINITIONS "serenity_main=sh_main")

serenity_bin(BuggieBox)
target_sources(BuggieBox PRIVATE main.cpp)
target_link_libraries(BuggieBox PRIVATE LibC LibMain LibBuggieBox)
foreach(file IN LISTS utility_srcs)
   buggiebox_utility(${file})
endforeach()

add_definitions(-D_BUGGIE_BOX)
