file(GLOB TEST_SOURCES  CONFIGURE_DEPENDS "*.cpp")

foreach(source ${TEST_SOURCES})
    serenity_test(${source} LibGfx LIBS LibGUI)
endforeach()
