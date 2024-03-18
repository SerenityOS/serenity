serenity_component(
    SpaceAnalyzer
    TARGETS SpaceAnalyzer
)

compile_gml(SpaceAnalyzer.gml SpaceAnalyzerGML.cpp)

set(SOURCES
    ProgressWindow.cpp
    SpaceAnalyzerGML.cpp
    Tree.cpp
    TreeMapWidget.cpp
    main.cpp
)

serenity_app(SpaceAnalyzer ICON app-space-analyzer)
target_link_libraries(SpaceAnalyzer PRIVATE LibCore LibDesktop LibFileSystem LibGfx LibGUI LibIPC LibMain LibURL)
