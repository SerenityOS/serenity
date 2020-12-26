set(SOURCES
    I386BackEnd.cpp
)

include_directories(../)
include_directories(../C++Compiler)

serenity_lib(LibBackEnd BackEnd)
target_link_libraries(LibBackEnd LibCore)