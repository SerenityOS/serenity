set(SOURCES
    main.cpp
    VBForm.cpp
    VBPropertiesWindow.cpp
    VBProperty.cpp
    VBWidget.cpp
    VBWidgetPropertyModel.cpp
    VBWidgetRegistry.cpp
)

serenity_app(VisualBuilder)
target_link_libraries(VisualBuilder LibGUI)
