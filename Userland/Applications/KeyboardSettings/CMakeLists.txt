set(SOURCES
    main.cpp
)

serenity_app(KeyboardSettings ICON app-keyboard-settings)
target_link_libraries(KeyboardSettings LibGUI LibKeyboard)
