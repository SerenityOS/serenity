set(SOURCES
    Client.cpp
)

set(GENERATED_SOURCES
    ../../Services/ContentAccessServer/ContentAccessClientEndpoint.h
    ../../Services/ContentAccessServer/ContentAccessServerEndpoint.h
)

serenity_lib(LibContentAccessClient contentaccessclient)
target_link_libraries(LibContentAccessClient PRIVATE LibCore LibIPC)
add_dependencies(LibContentAccessClient WindowServer)
