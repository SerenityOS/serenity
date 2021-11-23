serenity_component(
    Network.Applet
    REQUIRED
    TARGETS Network.Applet
)

set(SOURCES
    main.cpp
)

serenity_app(Network.Applet ICON network)
target_link_libraries(Network.Applet LibGUI LibCore LibGfx LibMain)
