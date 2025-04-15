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

# ===== LibC headers =====
# NOTE: We have to symlink LibC's headers into the sysroot to satisfy libc++'s include priority
#       requirements.
include(Headers.cmake)
link_libc_headers("${CMAKE_CURRENT_SOURCE_DIR}" "${CMAKE_STAGING_PREFIX}/${CMAKE_INSTALL_INCLUDEDIR}")

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
