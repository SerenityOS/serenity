add_subdirectory(LanguageServers)
add_subdirectory(LanguageClients)

set(SOURCES
    CodeDocument.cpp
    CursorTool.cpp
    Debugger/BacktraceModel.cpp
    Debugger/DebugInfoWidget.cpp
    Debugger/Debugger.cpp
    Debugger/DisassemblyModel.cpp
    Debugger/DisassemblyWidget.cpp
    Debugger/RegistersModel.cpp
    Debugger/VariablesModel.cpp
    Editor.cpp
    EditorWrapper.cpp
    FindInFilesWidget.cpp
    FormEditorWidget.cpp
    FormWidget.cpp
    Git/DiffViewer.cpp
    Git/GitFilesModel.cpp
    Git/GitFilesView.cpp
    Git/GitRepo.cpp
    Git/GitWidget.cpp
    HackStudioWidget.cpp
    LanguageClient.cpp
    Locator.cpp
    Project.cpp
    ProjectFile.cpp
    TerminalWrapper.cpp
    WidgetTool.cpp
    WidgetTreeModel.cpp
    main.cpp
)

serenity_app(HackStudio ICON app-hack-studio)
target_link_libraries(HackStudio LibWeb LibMarkdown LibGUI LibCpp LibGfx LibCore LibVT LibDebug LibX86 LibDiff LibShell)
add_dependencies(HackStudio CppLanguageServer)
