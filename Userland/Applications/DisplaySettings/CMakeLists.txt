serenity_component(
    DisplaySettings
    REQUIRED
    TARGETS DisplaySettings
)

stringify_gml(BackgroundSettings.gml BackgroundSettingsGML.h background_settings_gml)
stringify_gml(DesktopSettings.gml DesktopSettingsGML.h desktop_settings_gml)
stringify_gml(EffectsSettings.gml EffectsSettingsGML.h effects_settings_gml)
stringify_gml(FontSettings.gml FontSettingsGML.h font_settings_gml)
stringify_gml(MonitorSettings.gml MonitorSettingsGML.h monitor_settings_window_gml)
stringify_gml(ThemesSettings.gml ThemesSettingsGML.h themes_settings_gml)

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

set(GENERATED_SOURCES
    BackgroundSettingsGML.h
    DesktopSettingsGML.h
    EffectsSettingsGML.h
    FontSettingsGML.h
    MonitorSettingsGML.h
    ThemesSettingsGML.h
)

serenity_app(DisplaySettings ICON app-display-settings)
target_link_libraries(DisplaySettings PRIVATE LibCore LibDesktop LibGfx LibGUI LibConfig LibIPC LibMain LibEDID LibThreading LibFileSystemAccessClient LibURL)
