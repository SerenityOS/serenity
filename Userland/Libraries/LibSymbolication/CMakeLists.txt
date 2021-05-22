set(SOURCES
    Client.cpp
)

serenity_lib(LibSymbolication symbolication)
target_link_libraries(LibSymbolication LibDebug)
