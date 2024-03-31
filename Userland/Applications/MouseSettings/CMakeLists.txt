serenity_component(
    MouseSettings
    REQUIRED
    TARGETS MouseSettings
)

compile_gml(MouseWidget.gml MouseWidgetGML.cpp)
compile_gml(ThemeWidget.gml ThemeWidgetGML.cpp)
compile_gml(HighlightWidget.gml HighlightWidgetGML.cpp)

set(SOURCES
    DoubleClickArrowWidget.cpp
    HighlightPreviewWidget.cpp
    HighlightWidget.cpp
    HighlightWidgetGML.cpp
    MouseWidget.cpp
    MouseWidgetGML.cpp
    ThemeWidget.cpp
    ThemeWidgetGML.cpp
    main.cpp
)

serenity_app(MouseSettings ICON app-mouse)
target_link_libraries(MouseSettings PRIVATE LibCore LibGfx LibGUI LibIPC LibMain)
