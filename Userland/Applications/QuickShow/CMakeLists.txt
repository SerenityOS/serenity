set(SOURCES
    main.cpp
    QSWidget.cpp
)

serenity_app(QuickShow ICON filetype-image)
target_link_libraries(QuickShow LibDesktop LibGUI LibGfx)
