set(TEST_SOURCES
    TestEcho.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" Utilities)
endforeach()
