set(SOURCES
    Client.cpp
)

set(GENERATED_SOURCES
    ../../Services/FileSystemAccessServer/FileSystemAccessClientEndpoint.h
    ../../Services/FileSystemAccessServer/FileSystemAccessServerEndpoint.h
)

serenity_lib(LibFileSystemAccessClient filesystemaccessclient)
target_link_libraries(LibFileSystemAccessClient LibIPC)
add_dependencies(LibFileSystemAccessClient WindowServer)
