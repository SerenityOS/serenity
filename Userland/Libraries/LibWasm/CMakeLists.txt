set(SOURCES
    Parser/Parser.cpp
)

serenity_lib(LibWasm wasm)
target_link_libraries(LibWasm LibC LibCore)
