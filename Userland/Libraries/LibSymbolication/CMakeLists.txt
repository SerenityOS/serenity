set(SOURCES
    Client.cpp
)

serenity_lib(LibSymbolication symbolclient)
target_link_libraries(LibSymbolication LibDebug)
