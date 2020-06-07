set(SOURCES
    main.cpp
)

serenity_bin(KeyboardSettings)
target_link_libraries(KeyboardSettings LibGUI LibKeyboard)
