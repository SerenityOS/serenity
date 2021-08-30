serenity_component(
    Fifteen
    RECOMMENDED
    TARGETS Fifteen
)

set(SOURCES
    main.cpp
    BoardWidget.cpp
    CellWidget.cpp
    SettingsDialog.cpp
)

serenity_app(Fifteen ICON app-fifteen)
target_link_libraries(Fifteen LibGUI LibConfig)
