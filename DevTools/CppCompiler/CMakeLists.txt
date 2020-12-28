set(SOURCES
        main.cpp
)

serenity_bin(CppCompiler)

target_link_libraries(CppCompiler LibCpp LibIntermediate LibAssembly LibCore)
