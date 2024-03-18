serenity_component(
    PixelPaint
    RECOMMENDED
    TARGETS PixelPaint
    DEPENDS ImageDecoder FileSystemAccessServer
)

stringify_gml(PixelPaintWindow.gml PixelPaintWindowGML.h pixel_paint_window_gml)
stringify_gml(EditGuideDialog.gml EditGuideDialogGML.h edit_guide_dialog_gml)
stringify_gml(FilterGallery.gml FilterGalleryGML.h filter_gallery_gml)
stringify_gml(ResizeImageDialog.gml ResizeImageDialogGML.h resize_image_dialog_gml)
stringify_gml(LevelsDialog.gml LevelsDialogGML.h levels_dialog_gml)
stringify_gml(LuminosityMasking.gml LuminosityMaskingGML.h luminosity_masking_gml)
stringify_gml(ColorMasking.gml ColorMaskingGML.h color_masking_gml)
stringify_gml(Filters/MedianSettings.gml Filters/MedianSettingsGML.h median_settings_gml)

set(SOURCES
    CreateNewImageDialog.cpp
    CreateNewLayerDialog.cpp
    EditGuideDialog.cpp
    FilterGallery.cpp
    FilterTreeModel.cpp
    FilterPreviewWidget.cpp
    Filters/Bloom.cpp
    Filters/BoxBlur3.cpp
    Filters/BoxBlur5.cpp
    Filters/Filter.cpp
    Filters/FastBoxBlur.cpp
    Filters/HueAndSaturation.cpp
    Filters/GaussBlur3.cpp
    Filters/GaussBlur5.cpp
    Filters/Grayscale.cpp
    Filters/Invert.cpp
    Filters/LaplaceCardinal.cpp
    Filters/LaplaceDiagonal.cpp
    Filters/Median.cpp
    Filters/Sepia.cpp
    Filters/Sharpen.cpp
    HistogramWidget.cpp
    IconBag.cpp
    Image.cpp
    ImageEditor.cpp
    ImageMasking.cpp
    ImageProcessor.cpp
    Layer.cpp
    LayerListWidget.cpp
    LayerPropertiesWidget.cpp
    LevelsDialog.cpp
    MainWidget.cpp
    Mask.cpp
    PaletteWidget.cpp
    ProjectLoader.cpp
    ResizeImageDialog.cpp
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
    Tools/GradientTool.cpp
    Tools/LassoSelectTool.cpp
    Tools/LineTool.cpp
    Tools/MoveTool.cpp
    Tools/PenTool.cpp
    Tools/PickerTool.cpp
    Tools/PolygonalSelectTool.cpp
    Tools/RectangleSelectTool.cpp
    Tools/RectangleTool.cpp
    Tools/SprayTool.cpp
    Tools/TextTool.cpp
    Tools/Tool.cpp
    Tools/WandSelectTool.cpp
    Tools/ZoomTool.cpp
    VectorscopeWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    EditGuideDialogGML.h
    ColorMaskingGML.h
    FilterGalleryGML.h
    Filters/MedianSettingsGML.h
    LevelsDialogGML.h
    LuminosityMaskingGML.h
    PixelPaintWindowGML.h
    ResizeImageDialogGML.h
)

serenity_app(PixelPaint ICON app-pixel-paint)
target_link_libraries(PixelPaint PRIVATE LibCore LibImageDecoderClient LibGUI LibGfx LibFileSystemAccessClient LibConfig LibDesktop LibMain LibThreading LibURL)
