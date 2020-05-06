set(SOURCES
    AddEventDialog.cpp
    Calendar.cpp
    CalendarWidget.cpp
    main.cpp
)

serenity_bin(Calendar)
target_link_libraries(Calendar LibGUI)
