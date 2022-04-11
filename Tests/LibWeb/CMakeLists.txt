set(TEST_SOURCES
    TestHTMLTokenizer.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibWeb LIBS LibWeb)
endforeach()

install(FILES tokenizer-test.html DESTINATION usr/Tests/LibWeb)
