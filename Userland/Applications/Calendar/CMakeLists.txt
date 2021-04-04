compile_gml(CalendarWindow.gml CalendarWindowGML.h calendar_window_gml)
compile_gml(AddEventDialog.gml AddEventDialogGML.h add_event_dialog_gml)

set(SOURCES
    AddEventDialog.cpp
    AddEventDialogGML.h
    CalendarWindowGML.h
    main.cpp
)

serenity_app(Calendar ICON app-calendar)
target_link_libraries(Calendar LibGUI)
