serenity_component(
    Mouse
    TARGETS Mouse
)

set(SOURCES
    main.cpp
)

serenity_app(Mouse ICON app-mouse)
target_link_libraries(Mouse LibGUI LibGfx LibMain)
