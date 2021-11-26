set(TEST_SOURCES
    TestHTMLTokenizer.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibWeb LIBS LibWeb)
endforeach()

serenity_testjs_test(test-web.cpp test-web LIBS LibWeb)
install(TARGETS test-web RUNTIME DESTINATION bin OPTIONAL)
install(FILES tokenizer-test.html DESTINATION usr/Tests/LibWeb)
