serenity_component(
    Calendar
    TARGETS Calendar
)

compile_gml(CalendarWindow.gml CalendarWindowGML.cpp)

set(SOURCES
    AddEventDialog.cpp
    CalendarWindowGML.cpp
    main.cpp
)

serenity_app(Calendar ICON app-calendar)
target_link_libraries(Calendar PRIVATE LibConfig LibCore LibGfx LibGUI LibMain)
