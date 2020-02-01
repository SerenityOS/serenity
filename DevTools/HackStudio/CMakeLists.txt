set(SOURCES
    CppLexer.cpp
    CursorTool.cpp
    Editor.cpp
    EditorWrapper.cpp
    FindInFilesWidget.cpp
    FormEditorWidget.cpp
    FormWidget.cpp
    Locator.cpp
    main.cpp
    ProcessStateWidget.cpp
    Project.cpp
    ProjectFile.cpp
    TerminalWrapper.cpp
    Tool.cpp
    WidgetTool.cpp
    WidgetTreeModel.cpp
)

serenity_app(HackStudio)
target_link_libraries(HackStudio LibHTML LibMarkdown LibVT)
