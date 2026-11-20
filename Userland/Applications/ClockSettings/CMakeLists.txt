serenity_component(
    ClockSettings
    RECOMMENDED
    TARGETS ClockSettings
)

set(SOURCES
    main.cpp
    ClockSettingsWidget.cpp
    TimeZoneSettingsWidget.cpp
)

serenity_app(ClockSettings ICON app-analog-clock) # FIXME: Create a ClockSettings icon.
target_link_libraries(ClockSettings PRIVATE LibConfig LibCore LibGfx LibGUI LibMain LibLocale)
