set(SOURCES
    main.cpp
    Game.cpp
)

serenity_app(Conway ICON app-conway)
target_link_libraries(Conway LibGUI)
