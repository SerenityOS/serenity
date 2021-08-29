include(${SERENITY_PROJECT_ROOT}/Meta/CMake/commonmark_spec.cmake)
file(GLOB TEST_SOURCES  CONFIGURE_DEPENDS "*.cpp")

foreach(source ${TEST_SOURCES})
    serenity_test(${source} LibMarkdown LIBS LibMarkdown)
endforeach()
