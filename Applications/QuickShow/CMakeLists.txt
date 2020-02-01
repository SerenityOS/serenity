set(SOURCES
    main.cpp
    QSWidget.cpp
)

serenity_app(QuickShow)
target_link_libraries(QuickShow LibGUI)
