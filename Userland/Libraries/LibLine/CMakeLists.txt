set(SOURCES
    Editor.cpp
    InternalFunctions.cpp
    KeyCallbackMachine.cpp
    SuggestionManager.cpp
    XtermSuggestionDisplay.cpp
)

serenity_lib(LibLine line)
target_link_libraries(LibLine PRIVATE LibCoreMinimal LibCoreBasic LibUnicode LibFileSystem)

serenity_static_lib(LibLineStatic)
target_link_libraries(LibLineStatic PRIVATE LibUnicodeStatic LibCoreStatic)
