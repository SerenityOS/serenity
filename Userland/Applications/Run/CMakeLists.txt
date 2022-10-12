serenity_component(
    Run
    REQUIRED
    TARGETS Run
)

compile_gml(Run.gml RunGML.h run_gml)

set(SOURCES
    main.cpp
    RunWindow.cpp
)

set(GENERATED_SOURCES
    RunGML.h
)

serenity_app(Run ICON app-run)
target_link_libraries(Run LibCore LibDesktop LibGUI LibMain)
