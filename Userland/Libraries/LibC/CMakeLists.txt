set(SOURCES
    arch/${SERENITY_ARCH}/fenv.cpp
    arch/${SERENITY_ARCH}/setjmp.S
    arpa/inet.cpp
    assert.cpp
    ctype.cpp
    cxxabi.cpp
    dirent.cpp
    dlfcn.cpp
    fcntl.cpp
    fenv.cpp
    fnmatch.cpp
    ifaddrs.cpp
    getopt.cpp
    getsubopt.cpp
    glob.cpp
    grp.cpp
    inttypes.cpp
    ioctl.cpp
    langinfo.cpp
    libcinit.cpp
    libgen.cpp
    link.cpp
    locale.cpp
    malloc.cpp
    math.cpp
    mntent.cpp
    net.cpp
    netdb.cpp
    poll.cpp
    priority.cpp
    pthread.cpp
    pthread_cond.cpp
    pthread_integration.cpp
    pthread_once.cpp
    pthread_tls.cpp
    pty.cpp
    pwd.cpp
    qsort.cpp
    regex.cpp
    resolv.cpp
    scanf.cpp
    sched.cpp
    search.cpp
    semaphore.cpp
    serenity.cpp
    shadow.cpp
    signal.cpp
    spawn.cpp
    ssp.cpp
    stat.cpp
    stdio.cpp
    stdlib.cpp
    string.cpp
    strings.cpp
    sys/archctl.cpp
    sys/auxv.cpp
    sys/file.cpp
    sys/mman.cpp
    sys/prctl.cpp
    sys/ptrace.cpp
    sys/select.cpp
    sys/socket.cpp
    sys/statvfs.cpp
    sys/uio.cpp
    sys/wait.cpp
    syslog.cpp
    termios.cpp
    time.cpp
    times.cpp
    tls.cpp
    ulimit.cpp
    unistd.cpp
    utime.cpp
    utsname.cpp
    wchar.cpp
    wctype.cpp
    wstdio.cpp

    ${AK_SOURCES}
)

if (SERENITY_ARCH STREQUAL "x86_64")
    list(APPEND SOURCES
        arch/x86_64/memset.cpp
        arch/x86_64/memset.S
    )
endif()

# FIXME: This, ideally, should be an exhaustive list of headers (transitively) used by libc's own
#        headers. (Like, for example, we shouldn't include AK/Platform.h in them for no reason at
#        all.)
set(HEADERS
    arch/${SERENITY_ARCH}/fenv.h
    arch/fenv.h
    arpa/inet.h
    bits/dlfcn_integration.h
    bits/FILE.h
    bits/getopt.h
    bits/mutex_locker.h
    bits/posix1_lim.h
    bits/pthread_cancel.h
    bits/pthread_integration.h
    bits/search.h
    bits/sighow.h
    bits/stdint.h
    bits/stdio_file_implementation.h
    bits/utimens.h
    bits/wchar.h
    bits/wchar_size.h
    net/if.h
    net/if_arp.h
    net/route.h
    netinet/if_ether.h
    netinet/in.h
    netinet/in_systm.h
    netinet/ip.h
    netinet/ip_icmp.h
    netinet/tcp.h
    sys/arch/${SERENITY_ARCH}/regs.h
    sys/arch/regs.h
    sys/archctl.h
    sys/auxv.h
    sys/cdefs.h
    sys/device.h
    sys/devices/gpu.h
    sys/file.h
    sys/internals.h
    sys/ioctl.h
    sys/mman.h
    sys/param.h
    sys/poll.h
    sys/prctl.h
    sys/ptrace.h
    sys/resource.h
    sys/select.h
    sys/socket.h
    sys/stat.h
    sys/statvfs.h
    sys/sysmacros.h
    sys/time.h
    sys/times.h
    sys/ttydefaults.h
    sys/types.h
    sys/uio.h
    sys/un.h
    sys/utsname.h
    sys/wait.h
    alloca.h
    assert.h
    byteswap.h
    complex.h
    ctype.h
    dirent.h
    dlfcn.h
    elf.h
    endian.h
    errno_codes.h
    errno.h
    fcntl.h
    fd_set.h
    fenv.h
    float.h
    fnmatch.h
    getopt.h
    glob.h
    grp.h
    ifaddrs.h
    inttypes.h
    langinfo.h
    libgen.h
    limits.h
    link.h
    locale.h
    mallocdefs.h
    math.h
    memory.h
    mntent.h
    netdb.h
    nl_types.h
    paths.h
    poll.h
    pthread.h
    pty.h
    pwd.h
    regex.h
    resolv.h
    sched.h
    search.h
    semaphore.h
    serenity.h
    setjmp.h
    shadow.h
    signal.h
    spawn.h
    stdarg.h
    stdint.h
    stdio_ext.h
    stdio.h
    stdlib.h
    string.h
    strings.h
    sysexits.h
    syslog.h
    termios.h
    time.h
    ucontext.h
    ulimit.h
    unistd.h
    utime.h
    utmp.h
    wchar.h
    wctype.h

    ../LibELF/ELFABI.h
    ../LibRegex/RegexDefs.h
)

# ===== LibC headers =====
# NOTE: We have to symlink LibC's headers into the sysroot to satisfy libc++'s include priority
#       requirements.
foreach(RELATIVE_HEADER_PATH IN LISTS HEADERS)
    get_filename_component(directory ${RELATIVE_HEADER_PATH} DIRECTORY)
    get_filename_component(name ${RELATIVE_HEADER_PATH} NAME)
    string(REPLACE "../" "" subdirectory "${directory}")
    file(MAKE_DIRECTORY "${CMAKE_STAGING_PREFIX}/${CMAKE_INSTALL_INCLUDEDIR}/${subdirectory}")
    file(CREATE_LINK "${CMAKE_CURRENT_SOURCE_DIR}/${RELATIVE_HEADER_PATH}" "${CMAKE_STAGING_PREFIX}/${CMAKE_INSTALL_INCLUDEDIR}/${subdirectory}/${name}" SYMBOLIC)
endforeach()

# ===== Start files =====
# NOTE: We link all these against NoCoverage so that we don't break ports by requiring coverage
#       symbols in runtime/startup objects.
add_library(crt0 STATIC crt0.cpp)
target_link_libraries(crt0 PRIVATE NoCoverage)
add_custom_command(
    TARGET crt0 POST_BUILD
    COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_OBJECTS:crt0> ${CMAKE_INSTALL_PREFIX}/usr/lib/crt0.o
)

# FIXME: These files are never needed (and Clang driver never references them) but GCC might need
#        some convincing that this is the case.
if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    add_library(crt0_shared STATIC crt0_shared.cpp)
    target_link_libraries(crt0_shared PRIVATE NoCoverage)
    add_custom_command(
        TARGET crt0_shared POST_BUILD
        COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_OBJECTS:crt0_shared> ${CMAKE_INSTALL_PREFIX}/usr/lib/crt0_shared.o
    )

    add_library(crti STATIC arch/${SERENITY_ARCH}/crti.S)
    target_link_libraries(crti PRIVATE NoCoverage)
    add_custom_command(
        TARGET crti POST_BUILD
        COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_OBJECTS:crti> ${CMAKE_INSTALL_PREFIX}/usr/lib/crti.o
    )

    add_library(crtn STATIC arch/${SERENITY_ARCH}/crtn.S)
    target_link_libraries(crtn PRIVATE NoCoverage)
    add_custom_command(
        TARGET crtn POST_BUILD
        COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_OBJECTS:crtn> ${CMAKE_INSTALL_PREFIX}/usr/lib/crtn.o
    )
endif()

# ===== LibC =====
# Prevent GCC from removing null checks by marking the `FILE*` argument non-null
set_source_files_properties(stdio.cpp PROPERTIES COMPILE_FLAGS "-fno-builtin-fputc -fno-builtin-fputs -fno-builtin-fwrite")

# Prevent naively implemented string functions (like strlen) from being "optimized" into a call to themselves.
if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    set_source_files_properties(string.cpp wchar.cpp PROPERTIES COMPILE_FLAGS "-fno-tree-loop-distribution -fno-tree-loop-distribute-patterns")
else()
    set_source_files_properties(string.cpp wchar.cpp PROPERTIES COMPILE_FLAGS "-fno-builtin")
endif()

serenity_libc(LibC c)
add_dependencies(LibC crt0 LibUBSanitizer)
if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    add_dependencies(LibC crti crt0_shared crtn)
endif()
target_link_libraries(LibC PRIVATE LibSystem LibTimeZone)
target_link_options(LibC PRIVATE -nolibc)

# Provide a linker script instead of various other libraries that tells everything to link against LibC.
file(WRITE "${CMAKE_STAGING_PREFIX}/${CMAKE_INSTALL_LIBDIR}/libpthread.so" "INPUT(libc.so)")
file(WRITE "${CMAKE_STAGING_PREFIX}/${CMAKE_INSTALL_LIBDIR}/libdl.so" "INPUT(libc.so)")
file(WRITE "${CMAKE_STAGING_PREFIX}/${CMAKE_INSTALL_LIBDIR}/libm.so" "INPUT(libc.so)")
file(WRITE "${CMAKE_STAGING_PREFIX}/${CMAKE_INSTALL_LIBDIR}/libssp.so" "INPUT(libc.so)")

# ===== LibC for DynamicLoader =====
add_library(DynamicLoader_LibC STATIC ${SOURCES})
target_link_libraries(DynamicLoader_LibC
    PUBLIC DynamicLoader_CompileOptions
    PRIVATE DynamicLoader_LibSystem
)

# ===== Compatibility with the GCC toolchain without TARGET_LIBC_PROVIDES_SSP =====
# FIXME: Remove this on the next GCC toolchain version bump
if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    add_custom_target(create_compat_ssp_libraries)
    add_dependencies(Loader.so create_compat_ssp_libraries)

    add_custom_command(TARGET create_compat_ssp_libraries
        PRE_BUILD
        COMMAND "${CMAKE_COMMAND}" -E remove libssp.so
        COMMAND "${CMAKE_AR}" cr libssp.a
        WORKING_DIRECTORY "${CMAKE_STAGING_PREFIX}/${CMAKE_INSTALL_LIBDIR}"
    )

    add_custom_command(TARGET create_compat_ssp_libraries
        PRE_BUILD
        COMMAND "${CMAKE_COMMAND}" -E remove libssp_nonshared.a
        COMMAND "${CMAKE_AR}" cr libssp_nonshared.a
        WORKING_DIRECTORY "${CMAKE_STAGING_PREFIX}/${CMAKE_INSTALL_LIBDIR}"
    )
endif()
