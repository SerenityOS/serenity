set(SOURCES
    BucketTool.cpp
    CreateNewLayerDialog.cpp
    EllipseTool.cpp
    EraseTool.cpp
    Image.cpp
    ImageEditor.cpp
    Layer.cpp
    LayerModel.cpp
    LineTool.cpp
    main.cpp
    MoveTool.cpp
    PaletteWidget.cpp
    PenTool.cpp
    PickerTool.cpp
    RectangleTool.cpp
    SprayTool.cpp
    ToolboxWidget.cpp
    Tool.cpp
)

serenity_bin(PaintBrush)
target_link_libraries(PaintBrush LibGUI LibGfx)
