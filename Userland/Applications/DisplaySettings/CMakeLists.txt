compile_gml(DisplaySettingsWindow.gml DisplaySettingsWindowGML.h display_settings_window_gml)

set(SOURCES
    DisplaySettings.cpp
    DisplaySettingsWindowGML.h
    main.cpp
    MonitorWidget.cpp
)

serenity_app(DisplaySettings ICON app-display-settings)
target_link_libraries(DisplaySettings LibGUI)
