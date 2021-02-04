compile_ipc(SymbolServer.ipc SymbolServerEndpoint.h)
compile_ipc(SymbolClient.ipc SymbolClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    main.cpp
    SymbolServerEndpoint.h
    SymbolClientEndpoint.h
)

serenity_bin(SymbolServer)
target_link_libraries(SymbolServer LibIPC)
