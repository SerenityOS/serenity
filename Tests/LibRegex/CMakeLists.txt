set(TEST_SOURCES
    Benchmark.cpp
    Regex.cpp
    RegexLibC.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibRegex LIBS LibRegex)
endforeach()
