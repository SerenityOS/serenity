set(SOURCES
    Client.cpp
    Listener.cpp
)

set(GENERATED_SOURCES
    ../../Services/ConfigServer/ConfigServerEndpoint.h
    ../../Services/ConfigServer/ConfigClientEndpoint.h
)

serenity_lib(LibConfig config)
target_link_libraries(LibConfig PRIVATE LibCore LibIPC)
