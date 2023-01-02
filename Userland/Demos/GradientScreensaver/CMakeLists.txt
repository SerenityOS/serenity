serenity_component(
    GradientScreensaver
    TARGETS GradientScreensaver
)

set(SOURCES
    GradientScreensaver.cpp
)

serenity_app(GradientScreensaver ICON app-screensaver)
target_link_libraries(GradientScreensaver PRIVATE LibDesktop LibGUI LibCore LibGfx LibMain)
