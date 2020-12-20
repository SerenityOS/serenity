compile_gml(FileManagerWindow.gml FileManagerWindowGML.h file_manager_window_gml)

set(SOURCES
    DesktopWidget.cpp
    DirectoryView.cpp
    FileManagerWindowGML.h
    FileUtils.cpp
    main.cpp
    PropertiesDialog.cpp
)

serenity_app(FileManager ICON filetype-folder)
target_link_libraries(FileManager LibGUI LibDesktop)
