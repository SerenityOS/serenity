set(TEST_SOURCES
    TestCSSIDSpeed.cpp
    TestCSSPixels.cpp
    TestCSSTokenStream.cpp
    TestFetchInfrastructure.cpp
    TestFetchURL.cpp
    TestHTMLTokenizer.cpp
    TestMicrosyntax.cpp
    TestMimeSniff.cpp
    TestNumbers.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibWeb LIBS LibWeb)
endforeach()

target_link_libraries(TestFetchURL PRIVATE LibURL)

install(FILES tokenizer-test.html DESTINATION usr/Tests/LibWeb)
