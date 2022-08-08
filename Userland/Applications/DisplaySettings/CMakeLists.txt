serenity_component(
    DisplaySettings
    REQUIRED
    TARGETS DisplaySettings
)

compile_gml(BackgroundSettings.gml BackgroundSettingsGML.h background_settings_gml)
compile_gml(DesktopSettings.gml DesktopSettingsGML.h desktop_settings_gml)
compile_gml(EffectsSettings.gml EffectsSettingsGML.h effects_settings_gml)
compile_gml(FontSettings.gml FontSettingsGML.h font_settings_gml)
compile_gml(MonitorSettings.gml MonitorSettingsGML.h monitor_settings_window_gml)
compile_gml(ThemesSettings.gml ThemesSettingsGML.h themes_settings_gml)

set(SOURCES
    BackgroundSettingsGML.h
    BackgroundSettingsWidget.cpp
    DesktopSettingsWidget.cpp
    DesktopSettingsGML.h
    EffectsSettingsGML.h
    EffectsSettingsWidget.cpp
    FontSettingsGML.h
    FontSettingsWidget.cpp
    MonitorSettingsWidget.cpp
    MonitorSettingsGML.h
    MonitorWidget.cpp
    ThemePreviewWidget.h
    ThemePreviewWidget.cpp
    ThemesSettingsWidget.h
    ThemesSettingsWidget.cpp
    ThemesSettingsGML.h
    main.cpp
)

serenity_app(DisplaySettings ICON app-display-settings)
target_link_libraries(DisplaySettings LibDesktop LibGUI LibConfig LibMain LibEDID)
