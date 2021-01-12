set(SOURCES
    main.cpp
)

serenity_bin(UserName.MenuApplet)
target_link_libraries(UserName.MenuApplet LibGUI LibGfx)
