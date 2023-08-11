serenity_component(
    MasterWord
    RECOMMENDED
    TARGETS MasterWord
)

stringify_gml(MasterWord.gml MasterWordGML.h master_word_gml)

set(SOURCES
    main.cpp
    WordGame.cpp
)

set(GENERATED_SOURCES
    MasterWordGML.h
)

serenity_app(MasterWord ICON app-masterword)
target_link_libraries(MasterWord PRIVATE LibCore LibGfx LibGUI LibConfig LibMain LibDesktop)
