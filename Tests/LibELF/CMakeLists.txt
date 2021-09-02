set(TEST_SOURCES
    test-elf.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}"  LibELF)
endforeach()
