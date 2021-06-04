set(SOURCES
    AbstractMachine/AbstractMachine.cpp
    AbstractMachine/BytecodeInterpreter.cpp
    AbstractMachine/Configuration.cpp
    AbstractMachine/Interpreter.cpp
    Parser/Parser.cpp
    Printer/Printer.cpp
)

serenity_lib(LibWasm wasm)
target_link_libraries(LibWasm LibC LibCore)
