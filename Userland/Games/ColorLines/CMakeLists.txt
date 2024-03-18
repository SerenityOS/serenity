serenity_component(
    ColorLines
    RECOMMENDED
    TARGETS ColorLines
)

set(SOURCES
    ColorLines.cpp
    main.cpp
)

serenity_app(ColorLines ICON app-colorlines)
target_link_libraries(ColorLines PRIVATE LibGUI LibCore LibGfx LibConfig LibMain LibDesktop LibURL)
