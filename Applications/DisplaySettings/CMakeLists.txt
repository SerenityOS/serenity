set(SOURCES
    DisplaySettings.cpp
    main.cpp
    MonitorWidget.cpp
)

serenity_app(DisplaySettings ICON app-display-settings)
target_link_libraries(DisplaySettings LibGUI)
