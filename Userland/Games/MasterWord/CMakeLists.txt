serenity_component(
    MasterWord
    RECOMMENDED
    TARGETS MasterWord
)

set(SOURCES
    main.cpp
    WordGame.cpp
)

serenity_app(MasterWord ICON app-masterword)
target_link_libraries(MasterWord LibGUI LibConfig LibMain LibDesktop)
