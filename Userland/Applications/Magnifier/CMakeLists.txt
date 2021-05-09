set(SOURCES
    MagnifierWidget.cpp
    MagnifierWidget.h
    main.cpp
)

serenity_app(Magnifier ICON find)
target_link_libraries(Magnifier LibGUI)
