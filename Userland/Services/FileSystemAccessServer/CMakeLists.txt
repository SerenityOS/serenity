serenity_component(
    FileSystemAccessServer
    REQUIRED
    TARGETS FileSystemAccessServer
)

compile_ipc(FileSystemAccessServer.ipc FileSystemAccessServerEndpoint.h)
compile_ipc(FileSystemAccessClient.ipc FileSystemAccessClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    DownloadWidget.cpp
    DownloadWindow.cpp
    main.cpp
    FileSystemAccessServerEndpoint.h
    FileSystemAccessClientEndpoint.h
)

serenity_bin(FileSystemAccessServer)
target_link_libraries(FileSystemAccessServer LibCore LibIPC LibGUI LibProtocol LibDesktop)
add_dependencies(FileSystemAccessServer WindowServer)
