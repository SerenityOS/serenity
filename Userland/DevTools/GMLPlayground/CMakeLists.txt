serenity_component(
    GMLPlayground
    RECOMMENDED
    TARGETS GMLPlayground
)

set(SOURCES
    main.cpp
)

serenity_app(GMLPlayground ICON app-gml-playground)
target_link_libraries(GMLPlayground LibDesktop LibFileSystemAccessClient LibGUI LibMain)
