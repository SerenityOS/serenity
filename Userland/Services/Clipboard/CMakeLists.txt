serenity_component(
    Clipboard
    REQUIRED
    TARGETS Clipboard
)

compile_ipc(ClipboardServer.ipc ClipboardServerEndpoint.h)
compile_ipc(ClipboardClient.ipc ClipboardClientEndpoint.h)

set(SOURCES
    ConnectionFromClient.cpp
    Storage.cpp
    main.cpp
)

set(GENERATED_SOURCES
    ClipboardClientEndpoint.h
    ClipboardServerEndpoint.h
)

serenity_bin(Clipboard)
target_link_libraries(Clipboard PRIVATE LibCore LibIPC LibMain)
