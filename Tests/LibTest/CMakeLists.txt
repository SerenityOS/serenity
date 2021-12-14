set(TEST_SOURCES
    TestNoCrash.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibSQL LIBS LibSQL LibIPC)
endforeach()
