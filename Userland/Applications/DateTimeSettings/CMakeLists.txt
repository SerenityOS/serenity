serenity_component(
    DateTimeSettings
    REQUIRED
    TARGETS DateTimeSettings
)

compile_gml(DateTimeSettingsWindow.gml DateTimeSettingsWindowGML.h date_time_settings_window_gml)

set(SOURCES
    main.cpp
    DateTimeSettingsWindowGML.h
    DateTimeSettingsWindow.cpp
    DateTimeSettingsWindow.h
)

serenity_app(DateTimeSettings ICON app-date-time-settings)
target_link_libraries(DateTimeSettings LibGUI)
