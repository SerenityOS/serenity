serenity_component(
    Escalator
    REQUIRED
    TARGETS Escalator
)

compile_gml(Escalator.gml EscalatorGML.cpp)

set(SOURCES
    EscalatorGML.cpp
    main.cpp
    EscalatorWindow.cpp
)

serenity_app(Escalator ICON app-escalator)
target_link_libraries(Escalator PRIVATE LibCore LibDesktop LibGfx LibGUI LibMain)
