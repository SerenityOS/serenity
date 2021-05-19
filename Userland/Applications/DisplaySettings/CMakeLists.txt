compile_gml(MonitorSettings.gml MonitorSettingsGML.h monitor_settings_window_gml)
compile_gml(BackgroundSettings.gml BackgroundSettingsGML.h background_settings_gml)

set(SOURCES
    BackgroundSettingsGML.h
    BackgroundSettingsWidget.cpp
    MonitorSettingsWidget.cpp
    MonitorSettingsGML.h
    MonitorWidget.cpp
    main.cpp
)

serenity_app(DisplaySettings ICON app-display-settings)
target_link_libraries(DisplaySettings LibGUI)
