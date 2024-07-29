set(SOURCES
    Editor.cpp
    InternalFunctions.cpp
    KeyCallbackMachine.cpp
    SuggestionManager.cpp
    XtermSuggestionDisplay.cpp
)

serenity_lib(LibLine line)
target_link_libraries(LibLine PRIVATE LibCoreMinimal LibCoreBasic LibUnicode)

# ===== LibLine for BuggieBox =====
add_library(BuggieBox_LibLine STATIC ${SOURCES})
target_link_libraries(BuggieBox_LibLine
    PUBLIC StaticPIE_CompileOptions
    PRIVATE BuggieBox_LibCore BuggieBox_LibUnicode
)
