set(SOURCES
    ColorSpace.cpp
    CommonNames.cpp
    Document.cpp
    Encoding.cpp
    Encryption.cpp
    Filter.cpp
    Fonts/PDFFont.cpp
    Fonts/TrueTypeFont.cpp
    Fonts/Type0Font.cpp
    Fonts/Type1Font.cpp
    ObjectDerivatives.cpp
    Parser.cpp
    Reader.cpp
    Renderer.cpp
    Value.cpp
    )

serenity_lib(LibPDF pdf)
target_link_libraries(LibPDF LibC LibCore LibIPC LibGfx LibTextCodec LibCrypto)
