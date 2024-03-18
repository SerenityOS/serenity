serenity_component(
    Assistant
    RECOMMENDED
    TARGETS Assistant
)

set(SOURCES
    Providers.cpp
    main.cpp
    )

serenity_app(Assistant ICON app-assistant)
target_link_libraries(Assistant PRIVATE LibCore LibDesktop LibGfx LibGUI LibJS LibMain LibThreading LibURL)
