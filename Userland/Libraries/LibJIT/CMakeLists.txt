set(SOURCES
    InstructionBuffer.cpp
)

serenity_lib(LibJIT jit)
target_link_libraries(LibJIT LibC LibCore LibX86)
