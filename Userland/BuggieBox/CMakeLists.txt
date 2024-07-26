serenity_component(
    BuggieBox
    REQUIRED
    TARGETS BuggieBox
)

function (buggiebox_utility src)
    get_filename_component(utility ${src} NAME_WE)
    target_sources(BuggieBox PRIVATE ${src})
    set_source_files_properties(${src} PROPERTIES COMPILE_DEFINITIONS "serenity_main=${utility}_main")
endfunction()

set(utility_srcs
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
    ../Utilities/init.cpp
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

serenity_bin(BuggieBox)
target_sources(BuggieBox PRIVATE main.cpp)
target_link_libraries(BuggieBox PRIVATE LibMain LibShellMinimal LibArchive LibCompress LibCore LibCrypto LibELF LibFileSystem LibGfx LibLine LibRegex LibAudio LibURL)

foreach(file IN LISTS utility_srcs)
   buggiebox_utility(${file})
endforeach()

target_sources(BuggieBox PRIVATE ../Shell/main.cpp)
set_source_files_properties( ../Shell/main.cpp PROPERTIES COMPILE_DEFINITIONS "serenity_main=sh_main")
