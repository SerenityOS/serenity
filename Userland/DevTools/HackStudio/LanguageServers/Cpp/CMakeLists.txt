set(SOURCES
    ClientConnection.cpp
    main.cpp
    AutoComplete.cpp
)

set(GENERATED_SOURCES
    ../LanguageServerEndpoint.h
    ../LanguageClientEndpoint.h)

serenity_bin(CppLanguageServer)

# We link with LibGUI because we use GUI::TextDocument to update
# the content of files according to the edit actions we receive over IPC.
target_link_libraries(CppLanguageServer LibIPC LibCpp LibGUI)
