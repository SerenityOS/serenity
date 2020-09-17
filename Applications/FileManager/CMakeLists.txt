set(SOURCES
    DesktopWidget.cpp
    DirectoryView.cpp
    FileUtils.cpp
    main.cpp
    PropertiesDialog.cpp
)

serenity_bin(FileManager)
target_link_libraries(FileManager LibGUI LibDesktop)
