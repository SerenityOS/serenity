serenity_component(
    WorkspacePicker.Applet
    REQUIRED
    TARGETS WorkspacePicker.Applet
)

set(SOURCES
    DesktopStatusWindow.cpp
    main.cpp
)

serenity_bin(WorkspacePicker.Applet)
target_link_libraries(WorkspacePicker.Applet PRIVATE LibGUI LibCore LibGfx LibIPC LibMain)
