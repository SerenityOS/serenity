set(SOURCES
    CharacterBitmap.cpp
    Color.cpp
    DisjointRectSet.cpp
    Emoji.cpp
    Font.cpp
    GIFLoader.cpp
    GraphicsBitmap.cpp
    ImageDecoder.cpp
    Painter.cpp
    Palette.cpp
    PNGLoader.cpp
    Rect.cpp
    StylePainter.cpp
    SystemTheme.cpp
)

serenity_lib(LibDraw draw)
target_link_libraries(LibDraw LibCore LibM)
