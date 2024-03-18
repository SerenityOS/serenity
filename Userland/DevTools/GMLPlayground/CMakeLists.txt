serenity_component(
    GMLPlayground
    RECOMMENDED
    TARGETS GMLPlayground
)

stringify_gml(GMLPlaygroundWindow.gml GMLPlaygroundWindowGML.h gml_playground_window_gml)

set(SOURCES
    MainWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    GMLPlaygroundWindowGML.h
)

serenity_app(GMLPlayground ICON app-gml-playground)
target_link_libraries(GMLPlayground PRIVATE LibConfig LibCore LibDesktop LibFileSystemAccessClient LibGfx LibGUI LibMain LibSyntax LibURL)
