file(GLOB CMD_SOURCES CONFIGURE_DEPENDS "*.cpp")
file(GLOB LIBTEST_BASED_SOURCES "Test*.cpp")
list(REMOVE_ITEM CMD_SOURCES ${LIBTEST_BASED_SOURCES})

# FIXME: These tests do not use LibTest
foreach(CMD_SRC ${CMD_SOURCES})
    get_filename_component(CMD_NAME ${CMD_SRC} NAME_WE)
    add_executable(${CMD_NAME} ${CMD_SRC})
    target_link_libraries(${CMD_NAME} LibCore)
    install(TARGETS ${CMD_NAME} RUNTIME DESTINATION usr/Tests/Kernel/Legacy)
endforeach()


foreach(TEST_SRC ${LIBTEST_BASED_SOURCES})
    serenity_test(${TEST_SRC} Kernel)
endforeach()

target_link_libraries(elf-execve-mmap-race LibPthread)
target_link_libraries(kill-pidtid-confusion LibPthread)
target_link_libraries(nanosleep-race-outbuf-munmap LibPthread)
target_link_libraries(null-deref-close-during-select LibPthread)
target_link_libraries(null-deref-crash-during-pthread_join LibPthread)
target_link_libraries(uaf-close-while-blocked-in-read LibPthread)
target_link_libraries(pthread-cond-timedwait-example LibPthread)
