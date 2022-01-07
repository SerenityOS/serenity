serenity_component(
    Calendar
    TARGETS Calendar
)

compile_gml(CalendarWindow.gml CalendarWindowGML.h calendar_window_gml)

set(SOURCES
    AddEventDialog.cpp
    CalendarWindowGML.h
    main.cpp
)

serenity_app(Calendar ICON app-calendar)
target_link_libraries(Calendar LibGUI LibMain)
