set(SOURCES
    main.cpp
)

serenity_app(Network.MenuApplet ICON network)
target_link_libraries(Network.MenuApplet LibGUI LibCore LibGfx)
