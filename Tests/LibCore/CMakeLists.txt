set(TEST_SOURCES
    TestLibCoreArgsParser.cpp
    TestLibCoreFileWatcher.cpp
    TestLibCoreIODevice.cpp
    TestLibCoreDeferredInvoke.cpp
    TestLibCoreStream.cpp
    TestLibCoreFilePermissionsMask.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibCore)
endforeach()

# NOTE: Required because of the LocalServer tests
target_link_libraries(TestLibCoreStream LibThreading)

install(FILES long_lines.txt 10kb.txt small.txt DESTINATION usr/Tests/LibCore)
