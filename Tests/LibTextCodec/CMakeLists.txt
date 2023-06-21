set(TEST_SOURCES
    TestTextDecoders.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibTextCodec LIBS LibTextCodec)
endforeach()
