serenity_component(
    MouseSettings
    REQUIRED
    TARGETS MouseSettings
)

compile_gml(Mouse.gml MouseWidgetGML.h mouse_widget_gml)
compile_gml(Cursor.gml CursorWidgetGML.h cursor_widget_gml)
compile_gml(Theme.gml ThemeWidgetGML.h theme_widget_gml)

set(SOURCES
    main.cpp
    DoubleClickArrowWidget.cpp
    MouseWidget.cpp
    MouseWidget.h
    MouseWidgetGML.h
    CursorWidget.cpp
    CursorWidget.h
    CursorWidgetGML.h
    ThemeWidget.cpp
    ThemeWidget.h
    ThemeWidgetGML.h
)

serenity_app(MouseSettings ICON app-mouse)
target_link_libraries(MouseSettings LibGUI LibMain)
