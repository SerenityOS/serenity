serenity_component(
    Hearts
    DESCRIPTION "Hearts game"
    RECOMMENDED
    TARGETS Hearts
)

compile_gml(Hearts.gml HeartsGML.cpp)

set(SOURCES
    Game.cpp
    main.cpp
    Player.cpp
    ScoreCard.cpp
    SettingsDialog.cpp
    HeartsGML.cpp
)

serenity_app(Hearts ICON app-hearts)
target_link_libraries(Hearts PRIVATE LibCards LibGUI LibGfx LibCore LibConfig LibMain LibDesktop LibURL)
