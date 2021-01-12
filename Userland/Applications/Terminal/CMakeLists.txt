compile_gml(TerminalSettingsWindow.gml TerminalSettingsWindowGML.h terminal_settings_window_gml)

set(SOURCES
    TerminalSettingsWindowGML.h
    main.cpp
)

serenity_app(Terminal ICON app-terminal)
target_link_libraries(Terminal LibGUI LibVT)
