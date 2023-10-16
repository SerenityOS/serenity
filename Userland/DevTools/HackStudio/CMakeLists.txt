serenity_component(
    HackStudio
    RECOMMENDED
    TARGETS HackStudio
    DEPENDS CppLanguageServer ShellLanguageServer WebContent
)

add_subdirectory(LanguageServers)
add_subdirectory(LanguageClients)

stringify_gml(Dialogs/NewProjectDialog.gml Dialogs/NewProjectDialogGML.h new_project_dialog_gml)
stringify_gml(Dialogs/Git/GitCommitDialog.gml Dialogs/Git/GitCommitDialogGML.h git_commit_dialog_gml)

set(SOURCES
    CodeDocument.cpp
    ClassViewWidget.cpp
    Debugger/BacktraceModel.cpp
    Debugger/DebugInfoWidget.cpp
    Debugger/Debugger.cpp
    Debugger/DisassemblyModel.cpp
    Debugger/DisassemblyWidget.cpp
    Debugger/RegistersModel.cpp
    Debugger/VariablesModel.cpp
    DeclarationsModel.cpp
    Dialogs/Git/GitCommitDialog.cpp
    Dialogs/NewProjectDialog.cpp
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
    LanguageClient.cpp
    Locator.cpp
    Project.cpp
    ProjectBuilder.cpp
    ProjectConfig.cpp
    ProjectDeclarations.cpp
    ProjectFile.cpp
    ProjectTemplate.cpp
    TerminalWrapper.cpp
    ToDoEntries.cpp
    ToDoEntriesWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    Dialogs/Git/GitCommitDialogGML.h
    Dialogs/NewProjectDialogGML.h
)

serenity_app(HackStudio ICON app-hack-studio)
target_link_libraries(HackStudio PRIVATE LibELF LibWebView LibWeb LibMarkdown LibGUI LibCpp LibCMake LibGfx LibCore LibVT LibDebug LibDisassembly LibDiff LibShell LibSymbolication LibSyntax LibRegex LibSQL LibConfig LibCore LibCoredump LibDesktop LibFileSystem LibIPC LibJS LibMain LibThreading LibURL)
add_dependencies(HackStudio CppLanguageServer)
