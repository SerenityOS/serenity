set(SOURCES
    AddEventDialog.cpp
    main.cpp
)

serenity_app(Calendar ICON app-calendar)
target_link_libraries(Calendar LibGUI)
