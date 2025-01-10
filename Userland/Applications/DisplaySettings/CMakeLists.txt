serenity_component(
    DisplaySettings
    REQUIRED
    TARGETS DisplaySettings
)

compile_gml(BackgroundSettings.gml BackgroundSettingsGML.cpp)
compile_gml(DesktopSettings.gml DesktopSettingsGML.cpp)
compile_gml(EffectsSettings.gml EffectsSettingsGML.cpp)
compile_gml(FontSettings.gml FontSettingsGML.cpp)
compile_gml(MonitorSettings.gml MonitorSettingsGML.cpp)
compile_gml(ThemesSettings.gml ThemesSettingsGML.cpp)

set(SOURCES
    BackgroundSettingsWidget.cpp
    DesktopSettingsWidget.cpp
    EffectsSettingsWidget.cpp
    FontSettingsWidget.cpp
    MonitorSettingsWidget.cpp
    MonitorWidget.cpp
    ThemePreviewWidget.cpp
    ThemesSettingsWidget.cpp
    BackgroundSettingsGML.cpp
    DesktopSettingsGML.cpp
    EffectsSettingsGML.cpp
    FontSettingsGML.cpp
    MonitorSettingsGML.cpp
    ThemesSettingsGML.cpp
    main.cpp
)


serenity_app(DisplaySettings ICON app-display-settings)
target_link_libraries(DisplaySettings PRIVATE LibCore LibDesktop LibGfx LibGUI LibConfig LibIPC LibMain LibEDID LibThreading LibFileSystemAccessClient LibURL)
