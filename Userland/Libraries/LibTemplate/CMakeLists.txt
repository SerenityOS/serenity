set(SOURCES
    Template.cpp
    TemplateParser.cpp
)

serenity_lib(LibTemplate template)
target_link_libraries(LibTemplate LibC)
