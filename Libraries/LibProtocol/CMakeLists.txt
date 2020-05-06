set(SOURCES
    Client.cpp
    Download.cpp
)

set(GENERATED_SOURCES
    ../../Services/ProtocolServer/ProtocolClientEndpoint.h
    ../../Services/ProtocolServer/ProtocolServerEndpoint.h
)

serenity_lib(LibProtocol protocol)
target_link_libraries(LibProtocol LibIPC)
