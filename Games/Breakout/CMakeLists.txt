set(SOURCES
    main.cpp
    Game.cpp
    LevelSelectDialog.cpp
)

serenity_app(Breakout ICON app-breakout)
target_link_libraries(Breakout LibGUI)
