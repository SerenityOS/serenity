set(TEST_SOURCES
    TestRISCV64Disassembly.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibDisassembly LIBS LibDisassembly)
endforeach()
