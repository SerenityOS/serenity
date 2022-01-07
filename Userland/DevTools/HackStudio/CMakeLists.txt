serenity_component(
    HackStudio
    RECOMMENDED
    TARGETS HackStudio
    DEPENDS CppLanguageServer ShellLanguageServer
)

add_subdirectory(LanguageServers)
add_subdirectory(LanguageClients)

compile_gml(Dialogs/NewProjectDialog.gml Dialogs/NewProjectDialogGML.h new_project_dialog_gml)
compile_gml(Dialogs/Git/GitCommitDialog.gml Dialogs/Git/GitCommitDialogGML.h git_commit_dialog_gml)

set(SOURCES
    CodeDocument.cpp
    ClassViewWidget.cpp
    Debugger/BacktraceModel.cpp
    Debugger/DebugInfoWidget.cpp
    Debugger/Debugger.cpp
    Debugger/DebuggerGlobalJSObject.cpp
    Debugger/DebuggerVariableJSObject.cpp
    Debugger/DisassemblyModel.cpp
    Debugger/DisassemblyWidget.cpp
    Debugger/EvaluateExpressionDialog.cpp
    Debugger/RegistersModel.cpp
    Debugger/VariablesModel.cpp
    Dialogs/Git/GitCommitDialog.cpp
    Dialogs/Git/GitCommitDialogGML.h
    Dialogs/NewProjectDialog.cpp
    Dialogs/NewProjectDialogGML.h
    Dialogs/ProjectTemplatesModel.cpp
    Editor.cpp
    EditorWrapper.cpp
    FindInFilesWidget.cpp
    Git/DiffViewer.cpp
    Git/GitFilesModel.cpp
    Git/GitFilesView.cpp
    Git/GitRepo.cpp
    Git/GitWidget.cpp
    GMLPreviewWidget.cpp
    HackStudioWidget.cpp
    Language.cpp
    LanguageClient.cpp
    Locator.cpp
    Project.cpp
    ProjectBuilder.cpp
    ProjectDeclarations.cpp
    ProjectFile.cpp
    ProjectTemplate.cpp
    TerminalWrapper.cpp
    ToDoEntries.cpp
    ToDoEntriesWidget.cpp
    main.cpp
)

serenity_app(HackStudio ICON app-hack-studio)
target_link_libraries(HackStudio LibWeb LibMarkdown LibGUI LibCpp LibGfx LibCore LibVT LibDebug LibX86 LibDiff LibShell LibSymbolication LibRegex LibSQL LibCoredump LibMain)
link_with_unicode_data(HackStudio)
add_dependencies(HackStudio CppLanguageServer)
