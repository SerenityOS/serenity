set(SOURCES
    main.cpp
    InspectorWidget.cpp
)

serenity_app(Browser)
target_link_libraries(Browser LibHTML)
