serenity_component(
    CalendarSettings
    RECOMMENDED
    TARGETS CalendarSettings
)

stringify_gml(CalendarSettingsWidget.gml CalendarSettingsWidgetGML.h calendar_settings_widget_gml)

set(SOURCES
    main.cpp
    CalendarSettingsWidget.cpp
)

set(GENERATED_SOURCES
    CalendarSettingsWidgetGML.h
)

serenity_app(CalendarSettings ICON app-calendar)
target_link_libraries(CalendarSettings PRIVATE LibConfig LibCore LibGfx LibGUI LibMain)
