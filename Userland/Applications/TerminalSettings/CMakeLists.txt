serenity_component(
    TerminalSettings
    REQUIRED
    TARGETS TerminalSettings
)

stringify_gml(TerminalSettingsMain.gml TerminalSettingsMainGML.h terminal_settings_main_gml)
stringify_gml(TerminalSettingsView.gml TerminalSettingsViewGML.h terminal_settings_view_gml)

set(SOURCES
    TerminalSettingsWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    TerminalSettingsMainGML.h
    TerminalSettingsViewGML.h
)

serenity_app(TerminalSettings ICON app-terminal)
target_link_libraries(TerminalSettings PRIVATE LibCore LibGfx LibGUI LibConfig LibMain LibVT)
