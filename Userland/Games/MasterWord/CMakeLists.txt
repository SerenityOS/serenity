serenity_component(
    MasterWord
    RECOMMENDED
    TARGETS MasterWord
)

compile_gml(MasterWord.gml MasterWordGML.cpp master_word_gml)

set(SOURCES
    main.cpp
    WordGame.cpp
    MasterWordGML.cpp
)

serenity_app(MasterWord ICON app-masterword)
target_link_libraries(MasterWord PRIVATE LibCore LibGfx LibGUI LibConfig LibMain LibDesktop LibURL)
