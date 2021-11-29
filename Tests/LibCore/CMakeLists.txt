set(TEST_SOURCES
    TestLibCoreArgsParser.cpp
    TestLibCoreFileWatcher.cpp
    TestLibCoreIODevice.cpp
    TestLibCoreDeferredInvoke.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibCore)
endforeach()

install(FILES long_lines.txt 10kb.txt DESTINATION usr/Tests/LibCore)
