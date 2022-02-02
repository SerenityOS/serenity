serenity_component(
    ClockSettings
    RECOMMENDED
    TARGETS ClockSettings
)

compile_gml(ClockSettingsWidget.gml ClockSettingsWidgetGML.h clock_settings_widget_gml)

set(SOURCES
    main.cpp
    ClockSettingsWidget.cpp
    ClockSettingsWidget.h
    ClockSettingsWidgetGML.h
)

serenity_app(ClockSettings ICON app-analog-clock) # FIXME: Create a ClockSettings icon.
target_link_libraries(ClockSettings LibGUI LibMain LibUnicode)
link_with_unicode_data(ClockSettings)
