set(SOURCES
    AbstractMachine/AbstractMachine.cpp
    AbstractMachine/BytecodeInterpreter.cpp
    AbstractMachine/Configuration.cpp
    AbstractMachine/Validator.cpp
    Parser/Parser.cpp
    Printer/Printer.cpp
)

serenity_lib(LibWasm wasm)
target_link_libraries(LibWasm LibC LibCore)
