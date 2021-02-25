set(LIBC_SOURCES
    arpa/inet.cpp
    assert.cpp
    ctype.cpp
    cxxabi.cpp
    dirent.cpp
    dlfcn.cpp
    fcntl.cpp
    getopt.cpp
    grp.cpp
    ioctl.cpp
    libcinit.cpp
    libgen.cpp
    locale.cpp
    malloc.cpp
    mman.cpp
    mntent.cpp
    netdb.cpp
    poll.cpp
    pthread_integration.cpp
    pwd.cpp
    qsort.cpp
    scanf.cpp
    sched.cpp
    semaphore.cpp
    serenity.cpp
    signal.cpp
    spawn.cpp
    stat.cpp
    stdio.cpp
    stdlib.cpp
    string.cpp
    strings.cpp
    stubs.cpp
    syslog.cpp
    sys/prctl.cpp
    sys/ptrace.cpp
    sys/select.cpp
    sys/socket.cpp
    sys/uio.cpp
    sys/wait.cpp
    termcap.cpp
    termios.cpp
    time.cpp
    times.cpp
    ulimit.cpp
    unistd.cpp
    utime.cpp
    utsname.cpp
    wchar.cpp
)

file(GLOB AK_SOURCES CONFIGURE_DEPENDS "../../../AK/*.cpp")
file(GLOB ELF_SOURCES CONFIGURE_DEPENDS "../LibELF/*.cpp")

if ("${SERENITY_ARCH}" STREQUAL "i686")
    set(ASM_SOURCES "arch/i386/setjmp.S")
    set(ELF_SOURCES ${ELF_SOURCES} ../LibELF/Arch/i386/plt_trampoline.S)
elseif ("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(ASM_SOURCES "arch/x86_64/setjmp.S")
endif()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-unknown-warning-option -DSERENITY_LIBC_BUILD")

add_library(crt0 STATIC crt0.cpp)
add_custom_command(
    TARGET crt0
    COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_OBJECTS:crt0> ${CMAKE_INSTALL_PREFIX}/usr/lib/crt0.o
)
add_library(crt0_shared STATIC crt0_shared.cpp)
add_custom_command(
    TARGET crt0_shared
    COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_OBJECTS:crt0_shared> ${CMAKE_INSTALL_PREFIX}/usr/lib/crt0_shared.o
)

set_source_files_properties (ssp.cpp PROPERTIES COMPILE_FLAGS
    "-fno-stack-protector")
add_library(ssp STATIC ssp.cpp)
add_custom_command(
    TARGET ssp
    COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_OBJECTS:ssp> ${CMAKE_INSTALL_PREFIX}/usr/lib/ssp.o
)

set(SOURCES ${LIBC_SOURCES} ${AK_SOURCES} ${ELF_SOURCES} ${ASM_SOURCES})

serenity_libc_static(LibCStatic c)
target_link_libraries(LibCStatic crt0 ssp system)
add_dependencies(LibCStatic LibM LibSystem)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -static-libstdc++")
serenity_libc(LibC c)
target_link_libraries(LibC crt0 ssp system)
add_dependencies(LibC LibM LibSystem)
