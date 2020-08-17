set(SOURCES
    Editor.cpp
    InternalFunctions.cpp
    SuggestionManager.cpp
    XtermSuggestionDisplay.cpp
)

serenity_lib(LibLine line)
target_link_libraries(LibLine LibC LibCore)
