serenity_component(
    CppLanguageServer
    TARGETS CppLanguageServer
)

set(SOURCES
    main.cpp
)

set(GENERATED_SOURCES
    ../LanguageServerEndpoint.h
    ../LanguageClientEndpoint.h)

serenity_bin(CppLanguageServer)

# We link with LibGUI because we use GUI::TextDocument to update
# the content of files according to the edit actions we receive over IPC.
target_link_libraries(CppLanguageServer PRIVATE LibIPC LibCore LibCpp LibGUI LibLanguageServer LibCppComprehension LibMain LibRegex)
