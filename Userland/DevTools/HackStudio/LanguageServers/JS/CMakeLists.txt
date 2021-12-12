serenity_component(
    JSLanguageServer
    TARGETS JSLanguageServer
)

set(SOURCES
    JSComprehensionEngine.cpp
    main.cpp
)

set(GENERATED_SOURCES
    ../LanguageServerEndpoint.h
    ../LanguageClientEndpoint.h)

serenity_bin(JSLanguageServer)

# We link with LibGUI because we use GUI::TextDocument to update
# the content of files according to the edit actions we receive over IPC.
target_link_libraries(JSLanguageServer LibIPC LibJS LibGUI LibLanguageServer LibMain)
