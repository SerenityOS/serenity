serenity_component(
    ResourceGraph.Applet
    REQUIRED
    TARGETS ResourceGraph.Applet
)

set(SOURCES
    main.cpp
)

serenity_bin(ResourceGraph.Applet)
target_link_libraries(ResourceGraph.Applet PRIVATE LibGUI LibCore LibGfx LibMain)
