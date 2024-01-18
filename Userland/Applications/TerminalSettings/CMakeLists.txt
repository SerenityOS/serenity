serenity_component(
    TerminalSettings
    REQUIRED
    TARGETS TerminalSettings
)

compile_gml(TerminalSettingsMain.gml TerminalSettingsMainGML.cpp)
compile_gml(TerminalSettingsView.gml TerminalSettingsViewGML.cpp)

set(SOURCES
    TerminalSettingsMainGML.cpp
    TerminalSettingsViewGML.cpp
    MainWidget.cpp
    ViewWidget.cpp
    main.cpp
)

serenity_app(TerminalSettings ICON app-terminal)
target_link_libraries(TerminalSettings PRIVATE LibCore LibGfx LibGUI LibConfig LibMain LibVT)
