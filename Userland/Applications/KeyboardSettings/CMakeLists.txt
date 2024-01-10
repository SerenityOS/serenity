serenity_component(
    KeyboardSettings
    REQUIRED
    TARGETS KeyboardSettings
)

compile_gml(Keyboard.gml KeyboardWidgetGML.cpp)
compile_gml(KeymapDialog.gml KeymapDialogGML.cpp)

set(SOURCES
    KeyboardSettingsWidget.cpp
    main.cpp
    KeyboardWidgetGML.cpp
    KeymapDialogGML.cpp
)

serenity_app(KeyboardSettings ICON app-keyboard-settings)
target_link_libraries(KeyboardSettings PRIVATE LibCore LibGfx LibGUI LibKeyboard LibConfig LibMain)
