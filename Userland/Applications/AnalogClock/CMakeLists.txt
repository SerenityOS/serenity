set(SOURCES
        main.cpp
        AnalogClock.cpp
        )

serenity_app(AnalogClock ICON app-analog-clock)
target_link_libraries(AnalogClock LibGUI)
