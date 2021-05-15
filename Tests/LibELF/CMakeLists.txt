file(GLOB CMD_SOURCES  CONFIGURE_DEPENDS "*.cpp")

foreach(CMD_SRC ${CMD_SOURCES})
    serenity_test(${CMD_SRC} LibELF)
endforeach()
