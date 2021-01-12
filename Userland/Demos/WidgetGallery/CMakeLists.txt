set(SOURCES
    main.cpp
)

serenity_app(WidgetGallery ICON app-widget-gallery)
target_link_libraries(WidgetGallery LibGUI)
