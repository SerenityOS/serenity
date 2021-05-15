set(SOURCES
    Client.cpp
)

set(GENERATED_SOURCES
    ../../Services/SymbolServer/SymbolClientEndpoint.h
    ../../Services/SymbolServer/SymbolServerEndpoint.h
)

serenity_lib(LibSymbolClient symbolclient)
target_link_libraries(LibSymbolClient LibIPC)
