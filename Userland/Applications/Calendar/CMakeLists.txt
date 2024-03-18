serenity_component(
    Calendar
    TARGETS Calendar
)

compile_gml(CalendarWidget.gml CalendarWidgetGML.cpp)
compile_gml(AddEventDialog.gml AddEventDialogGML.cpp)
compile_gml(ViewEventDialog.gml ViewEventDialogGML.cpp)

set(SOURCES
    AddEventDialog.cpp
    AddEventDialogGML.cpp
    AddEventWidget.cpp
    CalendarWidget.cpp
    CalendarWidgetGML.cpp
    EventCalendar.cpp
    EventManager.cpp
    main.cpp
    ViewEventDialog.cpp
    ViewEventDialogGML.cpp
    ViewEventWidget.cpp
)

serenity_app(Calendar ICON app-calendar)
target_link_libraries(Calendar PRIVATE LibConfig LibCore LibFileSystem LibFileSystemAccessClient LibGfx LibGUI LibMain LibDesktop LibURL)
