set(SOURCES
    Client.cpp
)

set(GENERATED_SOURCES
    ../../Services/ConfigServer/ConfigServerEndpoint.h
    ../../Services/ConfigServer/ConfigClientEndpoint.h
)

serenity_lib(LibConfig config)
target_link_libraries(LibConfig LibIPC)
