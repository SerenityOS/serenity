serenity_component(
    DigitalClock
    RECOMMENDED
    TARGETS DigitalClock
)

set(SOURCES
        main.cpp
        DigitalClock.cpp
        )

serenity_app(DigitalClock ICON app-analog-clock)
target_link_libraries(DigitalClock LibGUI LibMain)
