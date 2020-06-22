set(SOURCES
    AffineTransform.cpp
    Bitmap.cpp
    BMPLoader.cpp
    CharacterBitmap.cpp
    Color.cpp
    DisjointRectSet.cpp
    Emoji.cpp
    FloatRect.cpp
    Font.cpp
    GIFLoader.cpp
    ICOLoader.cpp
    ImageDecoder.cpp
    JPGLoader.cpp
    Painter.cpp
    Palette.cpp
    Path.cpp
    PBMLoader.cpp
    PGMLoader.cpp
    PNGLoader.cpp
    PPMLoader.cpp
    Point.cpp
    Rect.cpp
    ShareableBitmap.cpp
    Size.cpp
    StylePainter.cpp
    SystemTheme.cpp
    Triangle.cpp
)

serenity_lib(LibGfx gfx)
target_link_libraries(LibGfx LibM LibCore)
