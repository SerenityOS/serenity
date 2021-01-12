compile_ipc(ClipboardServer.ipc ClipboardServerEndpoint.h)
compile_ipc(ClipboardClient.ipc ClipboardClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    ClipboardClientEndpoint.h
    ClipboardServerEndpoint.h
    Storage.cpp
    main.cpp
)

serenity_bin(Clipboard)
target_link_libraries(Clipboard LibCore LibIPC)
