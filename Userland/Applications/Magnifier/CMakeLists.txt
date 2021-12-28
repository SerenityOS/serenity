serenity_component(
    Magnifier
    RECOMMENDED
    TARGETS Magnifier
)

set(SOURCES
    MagnifierWidget.cpp
    MagnifierWidget.h
    main.cpp
)

serenity_app(Magnifier ICON app-magnifier)
target_link_libraries(Magnifier LibGfx LibGUI LibMain)
