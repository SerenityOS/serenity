serenity_component(
    WidgetGallery
    TARGETS WidgetGallery
)

set(SOURCES
    main.cpp
    GalleryWidget.cpp
    DemoWizardDialog.cpp
)

serenity_app(WidgetGallery ICON app-widget-gallery)
target_link_libraries(WidgetGallery PRIVATE LibCore LibGfx LibGUI LibMain LibFileSystemAccessClient LibIPC)
