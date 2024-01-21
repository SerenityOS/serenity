set(SOURCES
    Assembler.cpp
    GDB.cpp
)
if(NOT APPLE AND NOT WIN32 AND NOT EMSCRIPTEN)
    list(APPEND SOURCES GDBElf.cpp)
else()
    list(APPEND SOURCES GDBUnsupported.cpp)
endif()

serenity_lib(LibJIT jit)
if(NOT APPLE AND NOT WIN32 AND NOT EMSCRIPTEN)
    target_link_libraries(LibJIT PRIVATE LibELF)
endif()
