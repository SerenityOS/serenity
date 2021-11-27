serenity_component(
    Settings
    REQUIRED
    TARGETS Settings
)

set(SOURCES
    main.cpp
)

serenity_app(Settings ICON app-settings)
target_link_libraries(Settings LibGUI LibDesktop LibMain)
