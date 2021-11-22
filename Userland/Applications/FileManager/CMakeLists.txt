serenity_component(
    FileManager
    REQUIRED
    TARGETS FileManager
    DEPENDS FileOperation
)

compile_gml(FileManagerWindow.gml FileManagerWindowGML.h file_manager_window_gml)
compile_gml(FileOperationProgress.gml FileOperationProgressGML.h file_operation_progress_gml)
compile_gml(PropertiesWindowGeneralTab.gml PropertiesWindowGeneralTabGML.h properties_window_general_tab_gml)

set(SOURCES
    DesktopWidget.cpp
    DirectoryView.cpp
    FileManagerWindowGML.h
    FileOperationProgress.gml
    FileOperationProgressWidget.cpp
    FileUtils.cpp
    main.cpp
    PropertiesWindow.cpp
    PropertiesWindowGeneralTab.gml
)

serenity_app(FileManager ICON app-file-manager)
target_link_libraries(FileManager LibGUI LibDesktop LibConfig LibMain)
