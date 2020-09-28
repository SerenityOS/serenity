compile_ipc(CppLanguageServer.ipc CppLanguageServerEndpoint.h)
compile_ipc(CppLanguageClient.ipc CppLanguageClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    main.cpp
    CppLanguageServerEndpoint.h
    CppLanguageClientEndpoint.h
    AutoComplete.cpp
)

serenity_bin(CppLanguageServer)

# We link with LibGUI because we use GUI::TextDocument to update 
# the content of files according to the edit actions we receive over IPC.
target_link_libraries(CppLanguageServer LibIPC LibCpp LibGUI)
