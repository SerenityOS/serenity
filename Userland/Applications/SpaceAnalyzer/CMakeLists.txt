serenity_component(
    SpaceAnalyzer
    TARGETS SpaceAnalyzer
)

compile_gml(SpaceAnalyzer.gml SpaceAnalyzerGML.h space_analyzer_gml)

set(SOURCES
    TreeMapWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    SpaceAnalyzerGML.h
)

serenity_app(SpaceAnalyzer ICON app-space-analyzer)
target_link_libraries(SpaceAnalyzer LibDesktop LibGfx LibGUI LibMain)
