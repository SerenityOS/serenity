set(SOURCES
    Assembler.cpp
    GDB.cpp
)

serenity_lib(LibJIT jit)
target_link_libraries(LibJIT PRIVATE LibCore)
