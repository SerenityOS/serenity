serenity_component(
    Clipboard
    REQUIRED
    TARGETS Clipboard
)

compile_ipc(ClipboardServer.ipc ClipboardServerEndpoint.h)
compile_ipc(ClipboardClient.ipc ClipboardClientEndpoint.h)

set(SOURCES
    ConnectionFromClient.cpp
    ClipboardClientEndpoint.h
    ClipboardServerEndpoint.h
    Storage.cpp
    main.cpp
)

serenity_bin(Clipboard)
target_link_libraries(Clipboard LibCore LibIPC LibMain)
