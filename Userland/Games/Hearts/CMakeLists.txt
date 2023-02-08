serenity_component(
    Hearts
    DESCRIPTION "Hearts game"
    RECOMMENDED
    TARGETS Hearts
)

stringify_gml(Hearts.gml HeartsGML.h hearts_gml)

set(SOURCES
    Game.cpp
    main.cpp
    Player.cpp
    ScoreCard.cpp
    SettingsDialog.cpp
)

set(GENERATED_SOURCES
    HeartsGML.h
)

serenity_app(Hearts ICON app-hearts)
target_link_libraries(Hearts PRIVATE LibCards LibGUI LibGfx LibCore LibConfig LibMain LibDesktop)
