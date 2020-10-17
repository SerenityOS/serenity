set(SOURCES
    BrushTool.cpp
    BucketTool.cpp
    CreateNewLayerDialog.cpp
    EllipseTool.cpp
    EraseTool.cpp
    History.cpp
    Image.cpp
    ImageEditor.cpp
    Layer.cpp
    LayerListWidget.cpp
    LayerPropertiesWidget.cpp
    LineTool.cpp
    main.cpp
    MoveTool.cpp
    PaletteWidget.cpp
    PenTool.cpp
    PickerTool.cpp
    RectangleTool.cpp
    SprayTool.cpp
    ToolboxWidget.cpp
    ToolPropertiesWidget.cpp
    Tool.cpp
)

serenity_bin(PixelPaint)
target_link_libraries(PixelPaint LibGUI LibGfx)
