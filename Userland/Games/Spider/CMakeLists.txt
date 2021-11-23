serenity_component(
    Spider
    RECOMMENDED
    TARGETS Spider
)

compile_gml(Spider.gml SpiderGML.h spider_gml)

set(SOURCES
    Game.cpp
    main.cpp
    SpiderGML.h
)

serenity_app(Spider ICON app-spider)
target_link_libraries(Spider LibCards LibGUI LibGfx LibCore LibConfig LibMain)
