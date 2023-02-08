serenity_component(
    MouseSettings
    REQUIRED
    TARGETS MouseSettings
)

stringify_gml(Mouse.gml MouseWidgetGML.h mouse_widget_gml)
stringify_gml(Theme.gml ThemeWidgetGML.h theme_widget_gml)
stringify_gml(Highlight.gml HighlightWidgetGML.h highlight_widget_gml)

set(SOURCES
    DoubleClickArrowWidget.cpp
    HighlightPreviewWidget.cpp
    HighlightWidget.cpp
    MouseWidget.cpp
    ThemeWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    HighlightWidgetGML.h
    MouseWidgetGML.h
    ThemeWidgetGML.h
)

serenity_app(MouseSettings ICON app-mouse)
target_link_libraries(MouseSettings PRIVATE LibCore LibGfx LibGUI LibIPC LibMain)
