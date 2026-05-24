serenity_component(
    TriPeaks
    RECOMMENDED
    TARGETS TriPeaks
)

compile_gml(TriPeaks.gml TriPeaksGML.cpp tripeaks_gml)

set(SOURCES
    Game.cpp
    main.cpp
    TriPeaksGML.cpp
)

serenity_app(TriPeaks ICON app-tripeaks)
target_link_libraries(TriPeaks PRIVATE LibCards LibGUI LibGfx LibCore LibDesktop LibConfig LibMain LibURL)