serenity_component(
    TerminalSettings
    REQUIRED
    TARGETS TerminalSettings
)

compile_gml(TerminalSettingsMain.gml TerminalSettingsMainGML.h terminal_settings_main_gml)
compile_gml(TerminalSettingsView.gml TerminalSettingsViewGML.h terminal_settings_view_gml)

set(SOURCES
    main.cpp
    TerminalSettingsWidget.cpp
    TerminalSettingsMainGML.h
    TerminalSettingsViewGML.h
)

serenity_app(TerminalSettings ICON app-terminal)
target_link_libraries(TerminalSettings LibGUI LibConfig LibMain)
