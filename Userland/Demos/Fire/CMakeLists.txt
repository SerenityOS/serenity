serenity_component(
    Fire
    TARGETS Fire
)

set(SOURCES
    Fire.cpp
)

serenity_app(Fire ICON app-fire)
target_link_libraries(Fire PRIVATE LibGUI LibCore LibGfx LibMain)
