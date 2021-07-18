serenity_component(
    DesktopPicker.Applet
    REQUIRED
    TARGETS DesktopPicker.Applet
)

set(SOURCES
    DesktopStatusWindow.cpp
    main.cpp
)

serenity_bin(DesktopPicker.Applet)
target_link_libraries(DesktopPicker.Applet LibGUI LibCore LibGfx)
