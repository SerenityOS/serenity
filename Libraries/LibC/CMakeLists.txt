set(LIBC_SOURCES
    arpa/inet.cpp
    assert.cpp
    crt0.cpp
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
    pwd.cpp
    qsort.cpp
    scanf.cpp
    sched.cpp
    serenity.cpp
    setjmp.S
    signal.cpp
    spawn.cpp
    stat.cpp
    stdio.cpp
    stdlib.cpp
    string.cpp
    strings.cpp
    syslog.cpp
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

file(GLOB AK_SOURCES "../../AK/*.cpp")
file(GLOB ELF_SOURCES "../LibELF/*.cpp")
set(ELF_SOURCES ${ELF_SOURCES} ../LibELF/Arch/i386/plt_trampoline.S)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-unknown-warning-option -DSERENITY_LIBC_BUILD")

find_program(INSTALL_COMMAND ginstall)
if(NOT INSTALL_COMMAND)
    set(INSTALL_COMMAND install)
endif()

add_library(crt0 STATIC crt0.cpp)
add_custom_command(
    TARGET crt0
    COMMAND ${INSTALL_COMMAND} -D $<TARGET_OBJECTS:crt0> ${CMAKE_INSTALL_PREFIX}/usr/lib/crt0.o
)

set(SOURCES ${LIBC_SOURCES} ${AK_SOURCES} ${ELF_SOURCES})
serenity_libc(LibC c)
target_link_libraries(LibC crt0)
add_dependencies(LibC LibM)
