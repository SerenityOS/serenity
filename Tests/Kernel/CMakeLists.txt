set(TEST_SOURCES
    bind-local-socket-to-symlink.cpp
    bxvga-mmap-kernel-into-userspace.cpp
    crash-fcntl-invalid-cmd.cpp
    elf-execve-mmap-race.cpp
    elf-symbolication-kernel-read-exploit.cpp
    fuzz-syscalls.cpp
    kill-pidtid-confusion.cpp
    mmap-write-into-running-programs-executable-file.cpp
    mprotect-multi-region-mprotect.cpp
    munmap-multi-region-unmapping.cpp
    nanosleep-race-outbuf-munmap.cpp
    null-deref-close-during-select.cpp
    null-deref-crash-during-pthread_join.cpp
    path-resolution-race.cpp
    pthread-cond-timedwait-example.cpp
    setpgid-across-sessions-without-leader.cpp
    stress-truncate.cpp
    stress-writeread.cpp
    uaf-close-while-blocked-in-read.cpp
    unveil-symlinks.cpp
)

# FIXME: These tests do not use LibTest
foreach(source IN LISTS TEST_SOURCES)
    get_filename_component(test_name "${source}" NAME_WE)
    add_executable("${test_name}" "${source}")
    target_link_libraries("${test_name}" LibCore)
    install(TARGETS "${test_name}" RUNTIME DESTINATION usr/Tests/Kernel/Legacy)
endforeach()

serenity_test("crash.cpp" Kernel MAIN_ALREADY_DEFINED)

set(LIBTEST_BASED_SOURCES
    TestEFault.cpp
    TestKernelAlarm.cpp
    TestKernelFilePermissions.cpp
    TestKernelPledge.cpp
    TestKernelUnveil.cpp
    TestMemoryDeviceMmap.cpp
    TestMunMap.cpp
    TestProcFS.cpp
    TestProcFSWrite.cpp
    TestSigAltStack.cpp
    TestSigWait.cpp
)

foreach(libtest_source IN LISTS LIBTEST_BASED_SOURCES)
    serenity_test("${libtest_source}" Kernel)
endforeach()

target_link_libraries(elf-execve-mmap-race LibPthread)
target_link_libraries(kill-pidtid-confusion LibPthread)
target_link_libraries(nanosleep-race-outbuf-munmap LibPthread)
target_link_libraries(null-deref-close-during-select LibPthread)
target_link_libraries(null-deref-crash-during-pthread_join LibPthread)
target_link_libraries(uaf-close-while-blocked-in-read LibPthread)
target_link_libraries(pthread-cond-timedwait-example LibPthread)
