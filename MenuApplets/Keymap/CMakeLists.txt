set(SOURCES
    main.cpp
)

serenity_bin(Keymap.MenuApplet)
target_link_libraries(Keymap.MenuApplet LibGUI LibGfx LibKeyboard)
