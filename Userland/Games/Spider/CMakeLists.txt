serenity_component(
    Spider
    RECOMMENDED
    TARGETS Spider
)

compile_gml(Spider.gml SpiderGML.cpp spider_gml)

set(SOURCES
    Game.cpp
    main.cpp
    SpiderGML.cpp
)

serenity_app(Spider ICON app-spider)
target_link_libraries(Spider PRIVATE LibCards LibGUI LibGfx LibCore LibDesktop LibConfig LibMain LibURL)
