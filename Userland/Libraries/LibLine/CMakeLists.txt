set(SOURCES
    Editor.cpp
    InternalFunctions.cpp
    KeyCallbackMachine.cpp
    SuggestionManager.cpp
    XtermSuggestionDisplay.cpp
)

serenity_lib(LibLine line)
target_link_libraries(LibLine PRIVATE LibCore)

if (NOT BUILD_LAGOM AND NOT BUILD_LAGOM_TOOLS)
    serenity_lib_static(LibLineStatic line)
    target_link_libraries(LibLineStatic PRIVATE LibCoreStatic)
endif()
