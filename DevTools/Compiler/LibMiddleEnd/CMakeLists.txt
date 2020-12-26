set(SOURCES
    SIR.cpp
    Utils.cpp
)

include_directories(../C++Compiler)

serenity_lib(LibMiddleEnd MiddleEnd)
target_link_libraries(LibMiddleEnd LibCore)