serenity_component(
    GMLPlayground
    RECOMMENDED
    TARGETS GMLPlayground
)

compile_gml(GMLPlaygroundWindow.gml GMLPlaygroundWindowGML.h gml_playground_window_gml)

set(SOURCES
    main.cpp
)

set(GENERATED_SOURCES
    GMLPlaygroundWindowGML.h
)

serenity_app(GMLPlayground ICON app-gml-playground)
target_link_libraries(GMLPlayground PRIVATE LibCore LibDesktop LibFileSystemAccessClient LibGfx LibGUI LibMain LibSyntax)
