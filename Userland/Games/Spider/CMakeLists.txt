serenity_component(
    Spider
    RECOMMENDED
    TARGETS Spider
)

stringify_gml(Spider.gml SpiderGML.h spider_gml)

set(SOURCES
    Game.cpp
    main.cpp
)

set(GENERATED_SOURCES
    SpiderGML.h
)

serenity_app(Spider ICON app-spider)
target_link_libraries(Spider PRIVATE LibCards LibGUI LibGfx LibCore LibConfig LibMain)
