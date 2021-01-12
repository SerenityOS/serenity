set(SOURCES
    main.cpp
)

serenity_bin(Clock.MenuApplet)
target_link_libraries(Clock.MenuApplet LibGUI LibCore LibGfx)
