serenity_component(
    Eyes
    TARGETS Eyes
)

set(SOURCES
    main.cpp
    EyesWidget.cpp
)

serenity_app(Eyes ICON app-eyes)
target_link_libraries(Eyes PRIVATE LibCore LibDesktop LibGUI LibGfx LibMain LibURL)
