set(SOURCES
    SIR.cpp
    Utils.cpp
)


serenity_lib(LibIntermediate LibIntermediate)
target_link_libraries(LibIntermediate LibCore)
