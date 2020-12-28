set(SOURCES
    SIR.cpp
    Utils.cpp
)


serenity_lib(LibIntermediate MiddleEnd)
target_link_libraries(LibIntermediate LibCore)
