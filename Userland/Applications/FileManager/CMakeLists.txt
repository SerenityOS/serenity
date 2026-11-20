serenity_component(
    FileManager
    REQUIRED
    TARGETS FileManager
    DEPENDS FileOperation
)

set(SOURCES
    DesktopWidget.cpp
    DirectoryView.cpp
    FileOperationProgressWidget.cpp
    FileUtils.cpp
    main.cpp
    PropertiesWindow.cpp
)

serenity_app(FileManager ICON app-file-manager)
target_link_libraries(FileManager PRIVATE LibArchive LibAudio LibConfig LibCore LibDesktop LibFileSystem LibGfx LibGUI LibMain LibMaps LibPDF LibThreading LibURL)
