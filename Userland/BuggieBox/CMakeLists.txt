serenity_component(
    BuggieBox
    REQUIRED
    TARGETS BuggieBox
)

set(SOURCES
    main.cpp
    ../Shell/main.cpp
    ../Utilities/cat.cpp
    ../Utilities/checksum.cpp
    ../Utilities/chmod.cpp
    ../Utilities/chown.cpp
    ../Utilities/cp.cpp
    ../Utilities/df.cpp
    ../Utilities/env.cpp
    ../Utilities/file.cpp
    ../Utilities/find.cpp
    ../Utilities/id.cpp
    ../Utilities/less.cpp
    ../Utilities/ln.cpp
    ../Utilities/ls.cpp
    ../Utilities/lsblk.cpp
    ../Utilities/mkdir.cpp
    ../Utilities/mknod.cpp
    ../Utilities/mount.cpp
    ../Utilities/mv.cpp
    ../Utilities/ps.cpp
    ../Utilities/rm.cpp
    ../Utilities/rmdir.cpp
    ../Utilities/tail.cpp
    ../Utilities/tree.cpp
    ../Utilities/umount.cpp
    ../Utilities/uname.cpp
    ../Utilities/uniq.cpp
)

serenity_lib(LibBuggieBox buggiebox)
target_compile_definitions(LibBuggieBox PRIVATE -DEXCLUDE_SERENITY_MAIN)
target_link_libraries(LibBuggieBox PRIVATE LibShellStatic LibGfxStatic LibCoreStatic LibIPCStatic LibRegexStatic LibCompressStatic LibCryptoStatic LibLineStatic)

serenity_bin(BuggieBox)
target_link_libraries(BuggieBox PRIVATE LibC LibMain LibBuggieBox)
target_compile_definitions(BuggieBox PRIVATE -DEXCLUDE_SERENITY_MAIN)
