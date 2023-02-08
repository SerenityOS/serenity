serenity_component(
    KeyboardSettings
    REQUIRED
    TARGETS KeyboardSettings
)

stringify_gml(Keyboard.gml KeyboardWidgetGML.h keyboard_widget_gml)
stringify_gml(KeymapDialog.gml KeymapDialogGML.h keymap_dialog_gml)

set(SOURCES
    KeyboardSettingsWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    KeyboardWidgetGML.h
    KeymapDialogGML.h
)

serenity_app(KeyboardSettings ICON app-keyboard-settings)
target_link_libraries(KeyboardSettings PRIVATE LibCore LibGfx LibGUI LibKeyboard LibConfig LibMain)
