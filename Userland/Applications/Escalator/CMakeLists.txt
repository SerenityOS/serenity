serenity_component(
    Escalator
    REQUIRED
    TARGETS Escalator
)

compile_gml(Escalator.gml EscalatorGML.h escalator_gml)

set(SOURCES
    main.cpp
    EscalatorWindow.cpp
)

set(GENERATED_SOURCES
    EscalatorGML.h
)

serenity_app(Escalator ICON app-escalator)
target_link_libraries(Escalator LibCore LibDesktop LibGUI LibMain)
