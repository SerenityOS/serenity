serenity_component(
    TaskbarSettings
    REQUIRED
    TARGETS TaskbarSettings
)

compile_gml(TaskbarSettingsMain.gml TaskbarSettingsMainGML.h taskbar_settings_main_gml)

set(SOURCES
    main.cpp
    TaskbarSettingsMainGML.h
)

serenity_app(TaskbarSettings ICON app-settings)
target_link_libraries(TaskbarSettings LibGUI LibConfig LibMain)
