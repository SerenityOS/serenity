set(SOURCES
    main.cpp
)

serenity_bin(Network.MenuApplet)
target_link_libraries(Network.MenuApplet LibGUI LibCore LibGfx)
