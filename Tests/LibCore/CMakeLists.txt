set(TEST_SOURCES
    TestLibCoreArgsParser.cpp
    TestLibCoreFileWatcher.cpp
    TestLibCoreDeferredInvoke.cpp
    TestLibCoreStream.cpp
    TestLibCoreFilePermissionsMask.cpp
    TestLibCoreSharedSingleProducerCircularQueue.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibCore)
endforeach()

# NOTE: Required because of the LocalServer tests
target_link_libraries(TestLibCoreStream PRIVATE LibThreading)
target_link_libraries(TestLibCoreSharedSingleProducerCircularQueue PRIVATE LibThreading)

install(FILES long_lines.txt 10kb.txt small.txt DESTINATION usr/Tests/LibCore)
