serenity_component(
    WasmLanguageServer
    TARGETS WasmLanguageServer
)

set(SOURCES
    WasmComprehensionEngine.cpp
    main.cpp
)

set(GENERATED_SOURCES
    ../LanguageServerEndpoint.h
    ../LanguageClientEndpoint.h)

serenity_bin(WasmLanguageServer)

# We link with LibGUI because we use GUI::TextDocument to update
# the content of files according to the edit actions we receive over IPC.
target_link_libraries(WasmLanguageServer LibIPC LibWasm LibGUI LibLanguageServer LibMain)
