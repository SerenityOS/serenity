serenity_component(
    TaskbarSettings
    RECOMMENDED
    TARGETS TaskbarSettings
)

compile_gml(TaskbarSettingsWidget.gml TaskbarSettingsWidgetGML.h taskbar_settings_widget_gml)

set(SOURCES
    main.cpp
    TaskbarSettingsWidget.cpp
    TaskbarSettingsWidget.h
    TaskbarSettingsWidgetGML.h
)

serenity_app(TaskbarSettings ICON desktop)
target_link_libraries(TaskbarSettings LibConfig LibGUI LibMain)
