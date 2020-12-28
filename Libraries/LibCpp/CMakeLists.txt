set(SOURCES
        AST.cpp
        Driver.cpp
        IR.cpp
        Lexer.cpp
        Option.cpp
        Parser.cpp
)

add_subdirectory(LibAssembly)
add_subdirectory(LibIntermediate)

include_directories(LibIntermediate)
include_directories(LibAssembly)

serenity_lib(LibCpp cpp)
target_link_libraries(LibCpp LibIntermediate LibAssembly LibCore)
