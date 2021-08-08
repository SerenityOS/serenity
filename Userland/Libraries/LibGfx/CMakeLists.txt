compile_ipc(Remote/RemoteGfxClient.ipc Remote/RemoteGfxClientEndpoint.h)
compile_ipc(Remote/RemoteGfxServer.ipc Remote/RemoteGfxServerEndpoint.h)

set(GENERATED_SOURCES
    Remote/RemoteGfxClientEndpoint.h
    Remote/RemoteGfxServerEndpoint.h
)

set(SOURCES
    AffineTransform.cpp
    AntiAliasingPainter.cpp
    Bitmap.cpp
    BitmapFont.cpp
    BMPLoader.cpp
    BMPWriter.cpp
    CharacterBitmap.cpp
    ClassicStylePainter.cpp
    ClassicWindowTheme.cpp
    Color.cpp
    CursorParams.cpp
    DDSLoader.cpp
    DisjointRectSet.cpp
    Emoji.cpp
    Filters/FastBoxBlurFilter.cpp
    Font.cpp
    FontDatabase.cpp
    GIFLoader.cpp
    ICOLoader.cpp
    ImageDecoder.cpp
    JPGLoader.cpp
    OneBitBitmap.cpp
    Painter.cpp
    Palette.cpp
    Path.cpp
    PBMLoader.cpp
    PGMLoader.cpp
    PNGLoader.cpp
    PNGWriter.cpp
    PPMLoader.cpp
    Point.cpp
    Rect.cpp
    Remote/RemoteFontDatabase.cpp
    Remote/RemoteGfx.cpp
    Remote/RemoteGfxRenderer.cpp
    Remote/RemoteGfxServerConnection.cpp
    ShareableBitmap.cpp
    Size.cpp
    StylePainter.cpp
    SystemTheme.cpp
    TextDirection.cpp
    TextLayout.cpp
    Triangle.cpp
    TrueTypeFont/Font.cpp
    TrueTypeFont/Glyf.cpp
    TrueTypeFont/Cmap.cpp
    Typeface.cpp
    WindowTheme.cpp
)

serenity_lib(LibGfx gfx)
target_link_libraries(LibGfx LibM LibCompress LibCore LibTextCodec LibIPC)
