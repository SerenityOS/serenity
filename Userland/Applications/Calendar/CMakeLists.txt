serenity_component(
    Calendar
    TARGETS Calendar
)

compile_gml(CalendarWidget.gml CalendarWidgetGML.cpp)
compile_gml(AddEventDialog.gml AddEventDialogGML.cpp)

set(SOURCES
    AddEventDialog.cpp
    AddEventDialogGML.cpp
    AddEventWidget.cpp
    CalendarWidget.cpp
    CalendarWidgetGML.cpp
    EventCalendar.cpp
    EventManager.cpp
    main.cpp
)

serenity_app(Calendar ICON app-calendar)
target_link_libraries(Calendar PRIVATE LibConfig LibCore LibDateTime LibFileSystem LibFileSystemAccessClient LibGfx LibGUI LibMain LibDesktop)
