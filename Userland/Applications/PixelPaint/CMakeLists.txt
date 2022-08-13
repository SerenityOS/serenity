serenity_component(
    PixelPaint
    RECOMMENDED
    TARGETS PixelPaint
    DEPENDS ImageDecoder FileSystemAccessServer
)

compile_gml(PixelPaintWindow.gml PixelPaintWindowGML.h pixel_paint_window_gml)
compile_gml(EditGuideDialog.gml EditGuideDialogGML.h edit_guide_dialog_gml)
compile_gml(FilterGallery.gml FilterGalleryGML.h filter_gallery_gml)
compile_gml(ResizeImageDialog.gml ResizeImageDialogGML.h resize_image_dialog_gml)
compile_gml(LevelsDialog.gml LevelsDialogGML.h levels_dialog_gml)
compile_gml(Filters/MedianSettings.gml Filters/MedianSettingsGML.h median_settings_gml)

set(SOURCES
    CreateNewImageDialog.cpp
    CreateNewLayerDialog.cpp
    EditGuideDialog.cpp
    EditGuideDialogGML.h
    FilterGallery.cpp
    FilterGalleryGML.h
    FilterTreeModel.cpp
    FilterPreviewWidget.cpp
    Filters/Bloom.cpp
    Filters/BoxBlur3.cpp
    Filters/BoxBlur5.cpp
    Filters/Filter.cpp
    Filters/FastBoxBlur.cpp
    Filters/GaussBlur3.cpp
    Filters/GaussBlur5.cpp
    Filters/Grayscale.cpp
    Filters/Invert.cpp
    Filters/LaplaceCardinal.cpp
    Filters/LaplaceDiagonal.cpp
    Filters/Median.cpp
    Filters/MedianSettingsGML.h
    Filters/Sepia.cpp
    Filters/Sharpen.cpp
    HistogramWidget.cpp
    IconBag.cpp
    Image.cpp
    ImageEditor.cpp
    ImageProcessor.cpp
    Layer.cpp
    LayerListWidget.cpp
    LayerPropertiesWidget.cpp
    LevelsDialogGML.h
    LevelsDialog.cpp
    MainWidget.cpp
    Mask.cpp
    PaletteWidget.cpp
    PixelPaintWindowGML.h
    ProjectLoader.cpp
    ResizeImageDialog.cpp
    ResizeImageDialogGML.h
    ScopeWidget.cpp
    Selection.cpp
    ToolPropertiesWidget.cpp
    ToolboxWidget.cpp
    Tools/BrushTool.cpp
    Tools/BucketTool.cpp
    Tools/CloneTool.cpp
    Tools/EllipseTool.cpp
    Tools/EraseTool.cpp
    Tools/GuideTool.cpp
    Tools/LineTool.cpp
    Tools/MoveTool.cpp
    Tools/PenTool.cpp
    Tools/PickerTool.cpp
    Tools/RectangleSelectTool.cpp
    Tools/RectangleTool.cpp
    Tools/SprayTool.cpp
    Tools/Tool.cpp
    Tools/WandSelectTool.cpp
    Tools/ZoomTool.cpp
    VectorscopeWidget.cpp
    main.cpp
    )

serenity_app(PixelPaint ICON app-pixel-paint)
target_link_libraries(PixelPaint LibImageDecoderClient LibGUI LibGfx LibFileSystemAccessClient LibConfig LibMain)
