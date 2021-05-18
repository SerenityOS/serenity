compile_gml(FuzzySearch.gml FuzzySearchGML.h fuzzy_search_gml)

set(SOURCES
    main.cpp
    FuzzySearchGML.h
    FuzzyHaystackModel.h
)

serenity_app(FuzzySearch ICON app-fuzzy-search)
target_link_libraries(FuzzySearch LibGUI)
