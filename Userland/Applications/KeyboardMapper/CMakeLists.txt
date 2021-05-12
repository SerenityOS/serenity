set(SOURCES
    KeyboardMapperWidget.cpp
    KeyButton.cpp
    main.cpp
)

serenity_app(KeyboardMapper ICON app-keyboard-mapper)
target_link_libraries(KeyboardMapper LibGUI LibKeyboard)
