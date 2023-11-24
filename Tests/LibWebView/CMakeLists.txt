if (ENABLE_LAGOM_LIBWEB)

set(TEST_SOURCES
    TestWebViewURL.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibWebView LIBS LibWebView)
endforeach()

endif()
