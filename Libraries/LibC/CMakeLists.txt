set(LIBC_SOURCES
    stdio.cpp
    unistd.cpp
    string.cpp
    strings.cpp
    mman.cpp
    dirent.cpp
    malloc.cpp
    stdlib.cpp
    time.cpp
    utsname.cpp
    assert.cpp
    signal.cpp
    getopt.cpp
    scanf.cpp
    pwd.cpp
    grp.cpp
    times.cpp
    termcap.cpp
    stat.cpp
    mntent.cpp
    ctype.cpp
    fcntl.cpp
    termios.cpp
    ulimit.cpp
    qsort.cpp
    ioctl.cpp
    utime.cpp
    sys/select.cpp
    sys/socket.cpp
    sys/wait.cpp
    sys/uio.cpp
    poll.cpp
    locale.cpp
    arpa/inet.cpp
    netdb.cpp
    sched.cpp
    dlfcn.cpp
    libgen.cpp
    wchar.cpp
    serenity.cpp
    syslog.cpp
)

add_library(dummy OBJECT crt0.cpp)

add_custom_command(
    OUTPUT crt0.o
    COMMAND /bin/cp ${CMAKE_CURRENT_BINARY_DIR}/CMakeFiles/dummy.dir/crt0.cpp.obj crt0.o
)

add_custom_command(install.LibC
    ${CMAKE_COMMAND}
    -DBUILD_TYPE=${CMAKE_BUILD_TYPE}
    -P ${CMAKE_BINARY_DIR}/cmake_install.cmake)
add_dependencies(install.LibC main)

target_include_directories(dummy PUBLIC ${SERENITY_ROOT}/Libraries/LibC)
target_include_directories(dummy PUBLIC ${SERENITY_ROOT}/Libraries)
target_include_directories(dummy PUBLIC ${SERENITY_ROOT})

file(GLOB AK_SOURCES "../../AK/*.cpp")

set(SOURCES ${LIBC_SOURCES} ${AK_SOURCES} crt0.o)

serenity_libc(LibC c)
target_include_directories(LibC PUBLIC ${SERENITY_ROOT}/Libraries/LibC)
target_include_directories(LibC PUBLIC ${SERENITY_ROOT}/Libraries)
target_include_directories(LibC PUBLIC ${SERENITY_ROOT})
