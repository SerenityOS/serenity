serenity_component(
    LookupServer
    REQUIRED
    TARGETS LookupServer
)

compile_ipc(LookupServer.ipc LookupServerEndpoint.h)
compile_ipc(LookupClient.ipc LookupClientEndpoint.h)

set(SOURCES
    DNSAnswer.cpp
    DNSName.cpp
    DNSPacket.cpp
    DNSServer.cpp
    LookupServer.cpp
    LookupServerEndpoint.h
    LookupClientEndpoint.h
    ConnectionFromClient.cpp
    MulticastDNS.cpp
    main.cpp
)

serenity_bin(LookupServer)
target_link_libraries(LookupServer LibCore LibIPC LibMain)
