set(SOURCES
    Assembler.cpp
)

serenity_lib(LibJIT jit)
target_link_libraries(LibJIT PRIVATE LibCore)
