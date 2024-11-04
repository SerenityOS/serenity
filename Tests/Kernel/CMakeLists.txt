set(TEST_SOURCES
    bind-local-socket-to-symlink.cpp
    crash-fcntl-invalid-cmd.cpp
    elf-execve-mmap-race.cpp
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
    siginfo-example.cpp
    stress-truncate.cpp
    stress-writeread.cpp
    uaf-close-while-blocked-in-read.cpp
    unveil-symlinks.cpp
)

if (NOT CMAKE_SYSTEM_PROCESSOR STREQUAL "aarch64")
    list(APPEND TEST_SOURCES elf-symbolication-kernel-read-exploit.cpp)
endif()

# FIXME: These tests do not use LibTest
foreach(source IN LISTS TEST_SOURCES)
    get_filename_component(test_name "${source}" NAME_WE)
    add_executable("${test_name}" "${source}")
    target_link_libraries("${test_name}" PRIVATE LibCore)
    serenity_set_implicit_links("${test_name}")
    install(TARGETS "${test_name}" RUNTIME DESTINATION usr/Tests/Kernel/Legacy)
endforeach()

target_link_libraries(fuzz-syscalls PRIVATE LibSystem)

serenity_test("crash.cpp" Kernel MAIN_ALREADY_DEFINED)

set(LIBTEST_BASED_SOURCES
    TestAnonymousMmap.cpp
    TestEmptyPrivateInodeVMObject.cpp
    TestEmptySharedInodeVMObject.cpp
    TestExt2FS.cpp
    TestFileSystemDirentTypes.cpp
    TestInvalidUIDSet.cpp
    TestSFNUtilities.cpp
    TestSharedInodeVMObject.cpp
    TestPosixFallocate.cpp
    TestPrivateInodeVMObject.cpp
    TestKernelAlarm.cpp
    TestKernelFilePermissions.cpp
    TestKernelPledge.cpp
    TestKernelUnveil.cpp
    TestLoopDevice.cpp
    TestMunMap.cpp
    TestProcFS.cpp
    TestProcFSWrite.cpp
    TestSigAltStack.cpp
    TestSigHandler.cpp
    TestSigWait.cpp
    TestTCPSocket.cpp
)

if (ENABLE_KERNEL_COVERAGE_COLLECTION)
    list(APPEND LIBTEST_BASED_SOURCES TestKCOV.cpp)
endif()

if (CMAKE_SYSTEM_PROCESSOR STREQUAL "x86_64")
    list(APPEND LIBTEST_BASED_SOURCES TestMemoryDeviceMmap.cpp)
endif()

# FIXME: Don't panic on SafeMem access faults on aarch64
if (NOT CMAKE_SYSTEM_PROCESSOR STREQUAL "aarch64")
    list(APPEND LIBTEST_BASED_SOURCES TestEFault.cpp)
endif()

foreach(libtest_source IN LISTS LIBTEST_BASED_SOURCES)
    serenity_test("${libtest_source}" Kernel LIBS LibSystem)
endforeach()
