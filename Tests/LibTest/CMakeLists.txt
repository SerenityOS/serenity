set(TEST_SOURCES
    TestAsyncTestStreams.cpp
    TestNoCrash.cpp
    TestGenerator.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibSQL LIBS LibSQL LibIPC)
endforeach()
