serenity_component(
    TaskbarSettings
    REQUIRED
    TARGETS TaskbarSettings
)

compile_gml(TaskbarSettingsMain.gml TaskbarSettingsMainGML.h taskbar_settings_main_gml)
compile_gml(TaskbarSettingsClockView.gml TaskbarSettingsClockViewGML.h taskbar_settings_clock_view_gml)

set(SOURCES
    main.cpp
    ClockSettingsWidget.cpp
    TaskbarSettingsMainGML.h
    TaskbarSettingsClockViewGML.h
)

serenity_app(TaskbarSettings ICON app-settings)
target_link_libraries(TaskbarSettings LibGUI LibConfig LibMain)
