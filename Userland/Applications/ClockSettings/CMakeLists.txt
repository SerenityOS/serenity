serenity_component(
    ClockSettings
    RECOMMENDED
    TARGETS ClockSettings
)

compile_gml(ClockSettingsWidget.gml ClockSettingsWidgetGML.cpp)
compile_gml(TimeZoneSettingsWidget.gml TimeZoneSettingsWidgetGML.cpp)

set(SOURCES
    main.cpp
    ClockSettingsWidget.cpp
    TimeZoneSettingsWidget.cpp
    ClockSettingsWidgetGML.cpp
    TimeZoneSettingsWidgetGML.cpp
)

serenity_app(ClockSettings ICON app-analog-clock) # FIXME: Create a ClockSettings icon.
target_link_libraries(ClockSettings PRIVATE LibConfig LibCore LibGfx LibGUI LibMain LibLocale)
