serenity_component(
    GMLPlayground
    RECOMMENDED
    TARGETS GMLPlayground
)

set(SOURCES
    MainWidget.cpp
    main.cpp
)

serenity_app(GMLPlayground ICON app-gml-playground)
target_link_libraries(GMLPlayground PRIVATE LibConfig LibCore LibDesktop LibFileSystemAccessClient LibGfx LibGUI LibMain LibSyntax LibURL)
