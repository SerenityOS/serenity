set(SOURCES
    PreviewWidget.cpp
    main.cpp
)

serenity_bin(ThemeEditor)
target_link_libraries(ThemeEditor LibGUI)
