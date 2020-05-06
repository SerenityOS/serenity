set(SOURCES
    main.cpp
    QSWidget.cpp
)

serenity_bin(QuickShow)
target_link_libraries(QuickShow LibGUI LibGfx)
