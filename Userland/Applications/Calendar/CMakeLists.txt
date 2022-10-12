serenity_component(
    Calendar
    TARGETS Calendar
)

compile_gml(CalendarWindow.gml CalendarWindowGML.h calendar_window_gml)

set(SOURCES
    AddEventDialog.cpp
    main.cpp
)

set(GENERATED_SOURCES
    CalendarWindowGML.h
)

serenity_app(Calendar ICON app-calendar)
target_link_libraries(Calendar LibGUI LibMain)
