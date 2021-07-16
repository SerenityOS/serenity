file(GLOB CMD_SOURCES  CONFIGURE_DEPENDS "*.cpp")
add_compile_options(-fno-builtin)

foreach(CMD_SRC ${CMD_SOURCES})
    serenity_test(${CMD_SRC} LibM)
endforeach()
