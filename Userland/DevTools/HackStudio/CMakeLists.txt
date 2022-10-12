serenity_component(
    HackStudio
    RECOMMENDED
    TARGETS HackStudio
    DEPENDS CppLanguageServer ShellLanguageServer WebContent
)

add_subdirectory(LanguageServers)
add_subdirectory(LanguageClients)

compile_gml(Dialogs/NewProjectDialog.gml Dialogs/NewProjectDialogGML.h new_project_dialog_gml)
compile_gml(Dialogs/Git/GitCommitDialog.gml Dialogs/Git/GitCommitDialogGML.h git_commit_dialog_gml)
compile_gml(FindWidget.gml FindWidgetGML.h find_widget_gml)

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
    Dialogs/Git/GitCommitDialog.cpp
    Dialogs/NewProjectDialog.cpp
    Dialogs/ProjectTemplatesModel.cpp
    Editor.cpp
    EditorWrapper.cpp
    FindInFilesWidget.cpp
    FindWidget.cpp
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
    FindWidgetGML.h
)

serenity_app(HackStudio ICON app-hack-studio)
target_link_libraries(HackStudio LibWebView LibWeb LibMarkdown LibGUI LibCpp LibGfx LibCore LibVT LibDebug LibX86 LibDiff LibShell LibSymbolication LibRegex LibSQL LibCoredump LibMain)
add_dependencies(HackStudio CppLanguageServer)
