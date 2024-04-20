serenity_component(
    ClockSettings
    RECOMMENDED
    TARGETS ClockSettings
)

stringify_gml(ClockSettingsWidget.gml ClockSettingsWidgetGML.h clock_settings_widget_gml)
stringify_gml(TimeZoneSettingsWidget.gml TimeZoneSettingsWidgetGML.h time_zone_settings_widget_gml)

set(SOURCES
    main.cpp
    ClockSettingsWidget.cpp
    TimeZoneSettingsWidget.cpp
)

set(GENERATED_SOURCES
    ClockSettingsWidgetGML.h
    TimeZoneSettingsWidgetGML.h
)

serenity_app(ClockSettings ICON app-analog-clock) # FIXME: Create a ClockSettings icon.
target_link_libraries(ClockSettings PRIVATE LibConfig LibCore LibGfx LibGUI LibMain LibLocale)
