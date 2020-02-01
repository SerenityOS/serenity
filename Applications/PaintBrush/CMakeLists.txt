set(SOURCES
    main.cpp
    BucketTool.cpp
    EllipseTool.cpp
    EraseTool.cpp
    LineTool.cpp
    PaintableWidget.cpp
    PaletteWidget.cpp
    PenTool.cpp
    PickerTool.cpp
    RectangleTool.cpp
    SprayTool.cpp
    ToolboxWidget.cpp
    Tool.cpp
)

serenity_app(PaintBrush)
target_link_libraries(PaintBrush LibGUI)
