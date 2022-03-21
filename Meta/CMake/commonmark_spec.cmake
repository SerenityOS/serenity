option(ENABLE_COMMONMARK_SPEC_DOWNLOAD "Enable download of commonmark test suite at build time" ON)

set(MARKDOWN_TEST_PATH ${CMAKE_BINARY_DIR}/commonmark.spec.json)
set(MARKDOWN_TEST_URL https://spec.commonmark.org/0.30/spec.json)

if(ENABLE_COMMONMARK_SPEC_DOWNLOAD)
    file(DOWNLOAD ${MARKDOWN_TEST_URL} ${MARKDOWN_TEST_PATH})
    install(FILES ${MARKDOWN_TEST_PATH} DESTINATION home/anon/Tests)
endif()
