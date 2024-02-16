option(ENABLE_COMMONMARK_SPEC_DOWNLOAD "Enable download of commonmark test suite at build time" ON)

set(MARKDOWN_TEST_PATH ${SERENITY_CACHE_DIR}/commonmark.spec.json)
set(MARKDOWN_TEST_URL https://spec.commonmark.org/0.30/spec.json)

if(ENABLE_COMMONMARK_SPEC_DOWNLOAD)
    if (ENABLE_NETWORK_DOWNLOADS)
        download_file(${MARKDOWN_TEST_URL} ${MARKDOWN_TEST_PATH})
    else()
        message(STATUS "Skipping download of ${MARKDOWN_TEST_URL}, expecting the tests be at ${MARKDOWN_TEST_PATH}")
    endif()
    install(FILES ${MARKDOWN_TEST_PATH} DESTINATION home/anon/Tests)
endif()
