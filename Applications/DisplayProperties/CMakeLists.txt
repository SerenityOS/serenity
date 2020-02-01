set(SOURCES
    main.cpp
    DisplayProperties.cpp
)

serenity_app(DisplayProperties)
target_link_libraries(DisplayProperties LibGUI)
