serenity_component(
    ThemeEditor
    TARGETS ThemeEditor
)

set(SOURCES
    MainWidget.cpp
    PreviewWidget.cpp
    main.cpp
)

serenity_app(ThemeEditor ICON app-theme-editor)
target_link_libraries(ThemeEditor PRIVATE LibConfig LibCore LibGfx LibGUI LibFileSystem LibFileSystemAccessClient LibIPC LibMain LibURL)
