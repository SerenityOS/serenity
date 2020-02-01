set(SOURCES
    main.cpp
    DirectoryView.cpp
    FileUtils.cpp
    PropertiesDialog.cpp
)

serenity_app(FileManager)
target_link_libraries(FileManager LibGUI LibThread)
