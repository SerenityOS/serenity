set(LIBC_SOURCES
    arpa/inet.cpp
    assert.cpp
    ctype.cpp
    cxxabi.cpp
    dirent.cpp
    dlfcn.cpp
    fcntl.cpp
    fenv.cpp
    getopt.cpp
    grp.cpp
    inttypes.cpp
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
    set(ELF_SOURCES ${ELF_SOURCES} ../LibELF/Arch/x86_64/plt_trampoline.S)
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

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -static-libstdc++")
add_library(LibCStaticWithoutDeps STATIC ${SOURCES})
target_link_libraries(LibCStaticWithoutDeps crt0 ssp)
add_dependencies(LibCStaticWithoutDeps LibM LibSystem)

add_custom_target(LibCStatic
        COMMAND ${CMAKE_AR} -x $<TARGET_FILE:LibCStaticWithoutDeps>
        COMMAND ${CMAKE_AR} -x $<TARGET_FILE:ssp>
        COMMAND ${CMAKE_AR} -x $<TARGET_FILE:LibSystemStatic>
        COMMAND ${CMAKE_AR} -qcs ${CMAKE_CURRENT_BINARY_DIR}/libc.a *.o
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
        DEPENDS LibCStaticWithoutDeps ssp LibSystemStatic
        )

install(FILES ${CMAKE_CURRENT_BINARY_DIR}/libc.a DESTINATION ${CMAKE_INSTALL_PREFIX}/usr/lib/)
file(GLOB TEMP_OBJ_FILES ${CMAKE_CURRENT_BINARY_DIR}/*.o)
set_property(
        TARGET LibCStatic
        APPEND
        PROPERTY ADDITIONAL_CLEAN_FILES ${TEMP_OBJ_FILES}
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -static-libstdc++")
serenity_libc(LibC c)
target_link_libraries(LibC crt0 ssp system)

# We mark LibCStatic as a dependency of LibC because this triggers the build of the LibCStatic target
add_dependencies(LibC LibM LibSystem LibCStatic)
