serenity_component(
    BuggieBox
    REQUIRED
    TARGETS BuggieBox
)

serenity_component(
    BuggieBoxStatic
    REQUIRED
    TARGETS BuggieBoxStatic
)

function (buggiebox_utility buggiebox src)
    get_filename_component(utility ${src} NAME_WE)
    target_sources(${buggiebox} PRIVATE ${src})
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

serenity_static_bin(BuggieBoxStatic)
target_sources(BuggieBoxStatic PRIVATE main.cpp)
target_link_libraries(BuggieBoxStatic PRIVATE StaticPIE_CompileOptions LibStaticPIE)

if (ENABLE_UNDEFINED_SANITIZER)
    target_link_libraries(BuggieBoxStatic PRIVATE LibUBSanitizerStatic)
endif()

# If we don't include the whole archive, the linker might miss some weak symbols to override such as pthread_cond_broadcast.
target_link_libraries(BuggieBoxStatic PRIVATE
    $<LINK_LIBRARY:WHOLE_ARCHIVE,LibCStatic>
)

# Export __libc_init function so the dynamic loader can find it and use it
# We do this to avoid having ifdef soup for static-PIE ELFs (or just ELFs that embed LibC for some reason)
target_link_options(BuggieBoxStatic PRIVATE
    -Wl,--dynamic-list=${CMAKE_SOURCE_DIR}/Userland/BuggieBox/function_exports.txt
)

target_link_libraries(BuggieBoxStatic PRIVATE LibMainStatic 
                    LibSystem LibShellMinimalStatic LibCoreMinimalStatic LibCoreBasicStatic
                    LibELFStatic LibArchiveStatic LibAudioStatic LibGfxStatic LibTextCodecStatic)

                    
foreach(file IN LISTS utility_srcs)
   buggiebox_utility(BuggieBox ${file})
endforeach()

foreach(file IN LISTS utility_srcs)
   buggiebox_utility(BuggieBoxStatic ${file})
endforeach()

target_sources(BuggieBox PRIVATE ../Shell/main.cpp)
target_sources(BuggieBoxStatic PRIVATE ../Shell/main.cpp)
set_source_files_properties( ../Shell/main.cpp PROPERTIES COMPILE_DEFINITIONS "serenity_main=sh_main")
