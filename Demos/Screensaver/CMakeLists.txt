set(SOURCES
    Screensaver.cpp
)

serenity_bin(Screensaver)
target_link_libraries(Screensaver LibGUI LibCore LibGfx)
