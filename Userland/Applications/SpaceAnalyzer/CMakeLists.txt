serenity_component(
    SpaceAnalyzer
    TARGETS SpaceAnalyzer
)

stringify_gml(SpaceAnalyzer.gml SpaceAnalyzerGML.h space_analyzer_gml)

set(SOURCES
    ProgressWindow.cpp
    Tree.cpp
    TreeMapWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    SpaceAnalyzerGML.h
)

serenity_app(SpaceAnalyzer ICON app-space-analyzer)
target_link_libraries(SpaceAnalyzer PRIVATE LibCore LibDesktop LibFileSystem LibGfx LibGUI LibIPC LibMain)
