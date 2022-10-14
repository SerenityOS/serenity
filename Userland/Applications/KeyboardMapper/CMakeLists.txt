serenity_component(
    KeyboardMapper
    RECOMMENDED
    TARGETS KeyboardMapper
)

set(SOURCES
    KeyboardMapperWidget.cpp
    KeyButton.cpp
    main.cpp
)

serenity_app(KeyboardMapper ICON app-keyboard-mapper)
target_link_libraries(KeyboardMapper PRIVATE LibCore LibGfx LibGUI LibKeyboard LibMain)
