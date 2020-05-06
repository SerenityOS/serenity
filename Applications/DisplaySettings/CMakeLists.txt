set(SOURCES
    DisplaySettings.cpp
    main.cpp
    MonitorWidget.cpp
)

serenity_bin(DisplaySettings)
target_link_libraries(DisplaySettings LibGUI)
