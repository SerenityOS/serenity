serenity_component(
    DisplaySettings
    REQUIRED
    TARGETS DisplaySettings
)

set(SOURCES
    BackgroundSettingsWidget.cpp
    DesktopSettingsWidget.cpp
    EffectsSettingsWidget.cpp
    FontSettingsWidget.cpp
    MonitorSettingsWidget.cpp
    MonitorWidget.cpp
    ThemePreviewWidget.cpp
    ThemesSettingsWidget.cpp
    main.cpp
)

serenity_app(DisplaySettings ICON app-display-settings)
target_link_libraries(DisplaySettings PRIVATE LibCore LibDesktop LibGfx LibGUI LibConfig LibIPC LibMain LibEDID LibThreading LibFileSystemAccessClient LibURL)
