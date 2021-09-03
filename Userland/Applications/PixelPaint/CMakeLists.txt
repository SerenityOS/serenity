serenity_component(
    PixelPaint
    RECOMMENDED
    TARGETS PixelPaint
    DEPENDS ImageDecoder FileSystemAccessServer
)

compile_gml(PixelPaintWindow.gml PixelPaintWindowGML.h pixel_paint_window_gml)
compile_gml(EditGuideDialog.gml EditGuideDialogGML.h edit_guide_dialog_gml)

set(SOURCES
    BrushTool.cpp
    BucketTool.cpp
    CreateNewImageDialog.cpp
    CreateNewLayerDialog.cpp
    EditGuideDialog.cpp
    EditGuideDialogGML.h
    EllipseTool.cpp
    EraseTool.cpp
    GuideTool.cpp
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
    PixelPaintWindowGML.h
    ProjectLoader.cpp
    RectangleTool.cpp
    RectangleSelectTool.cpp
    Mask.cpp
    Selection.cpp
    SprayTool.cpp
    ToolboxWidget.cpp
    ToolPropertiesWidget.cpp
    Tool.cpp
    ZoomTool.cpp
)

serenity_app(PixelPaint ICON app-pixel-paint)
target_link_libraries(PixelPaint LibImageDecoderClient LibGUI LibGfx LibFileSystemAccessClient)
