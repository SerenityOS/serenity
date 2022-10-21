serenity_component(
    LookupServer
    REQUIRED
    TARGETS LookupServer
)

compile_ipc(LookupServer.ipc LookupServerEndpoint.h)
compile_ipc(LookupClient.ipc LookupClientEndpoint.h)

set(SOURCES
    DNSServer.cpp
    LookupServer.cpp
    ConnectionFromClient.cpp
    MulticastDNS.cpp
    main.cpp
)

set(GENERATED_SOURCES
    LookupServerEndpoint.h
    LookupClientEndpoint.h
)

serenity_bin(LookupServer)
target_link_libraries(LookupServer PRIVATE LibCore LibDNS LibIPC LibMain)
