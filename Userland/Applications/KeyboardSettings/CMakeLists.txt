serenity_component(
    KeyboardSettings
    REQUIRED
    TARGETS KeyboardSettings
)

compile_gml(Keyboard.gml KeyboardWidgetGML.h keyboard_widget_gml)

set(SOURCES
    main.cpp
    KeyboardSettingsWidget.cpp
    KeyboardWidgetGML.h
)

serenity_app(KeyboardSettings ICON app-keyboard-settings)
target_link_libraries(KeyboardSettings LibGUI LibKeyboard LibConfig LibMain)
