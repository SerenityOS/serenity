serenity_component(
    MouseSettings
    REQUIRED
    TARGETS MouseSettings
)

compile_gml(MouseSettingsWindow.gml MouseSettingsWindowGML.h mouse_settings_window_gml)

set(SOURCES
    main.cpp
    DoubleClickArrowWidget.cpp
    MouseSettingsWindow.cpp
    MouseSettingsWindow.h
    MouseSettingsWindowGML.h
)

serenity_app(MouseSettings ICON app-mouse)
target_link_libraries(MouseSettings LibGUI)
