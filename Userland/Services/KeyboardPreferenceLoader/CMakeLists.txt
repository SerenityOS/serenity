set(SOURCES
    main.cpp
)

serenity_bin(KeyboardPreferenceLoader)
target_link_libraries(KeyboardPreferenceLoader LibCore)
