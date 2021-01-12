set(SOURCES
    main.cpp
)

serenity_bin(ResourceGraph.MenuApplet)
target_link_libraries(ResourceGraph.MenuApplet LibGUI LibCore LibGfx)
