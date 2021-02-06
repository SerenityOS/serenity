compile_ipc(LookupServer.ipc LookupServerEndpoint.h)
compile_ipc(LookupClient.ipc LookupClientEndpoint.h)

set(SOURCES
    DNSAnswer.cpp
    DNSPacket.cpp
    LookupServer.cpp
    LookupServerEndpoint.h
    LookupClientEndpoint.h
    ClientConnection.cpp
    main.cpp
)

serenity_bin(LookupServer)
target_link_libraries(LookupServer LibCore LibIPC)
