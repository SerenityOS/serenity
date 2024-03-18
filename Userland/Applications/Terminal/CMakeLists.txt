serenity_component(
    Terminal
    REQUIRED
    TARGETS Terminal utmpupdate
)

set(SOURCES
    main.cpp
)

serenity_app(Terminal ICON app-terminal)
target_link_libraries(Terminal PRIVATE LibConfig LibCore LibFileSystem LibDesktop LibGfx LibGUI LibVT LibMain LibURL)
