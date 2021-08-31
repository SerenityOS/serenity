serenity_component(
    Screenshot
    TARGETS Screenshot
)

compile_gml(ScreenshotWindow.gml ScreenshotWindowGML.h screenshot_window_gml)

set(SOURCES
    ScreenshotWindowGML.h
    SelectableOverlay.h
    ScreenshotWidget.cpp
    ScreenshotWidget.h
    main.cpp
)

serenity_app(Screenshot ICON app-screenshot)
target_link_libraries(Screenshot LibGUI LibConfig)
