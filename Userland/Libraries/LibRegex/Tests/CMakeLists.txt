file(GLOB TEST_SOURCES CONFIGURE_DEPENDS "*.cpp")
file(GLOB REGEX_SOURCES CONFIGURE_DEPENDS "../*.cpp" "../C/*.cpp")

foreach(source ${TEST_SOURCES})
    serenity_test(${source} LibRegex LIBS LibRegex)
endforeach()
