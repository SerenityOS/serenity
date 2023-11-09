set(SOURCES
    ColorSpace.cpp
    CommonNames.cpp
    Document.cpp
    DocumentParser.cpp
    Encoding.cpp
    Encryption.cpp
    Filter.cpp
    Fonts/AdobeGlyphList.cpp
    Fonts/CFF.cpp
    Fonts/PDFFont.cpp
    Fonts/PS1FontProgram.cpp
    Fonts/SimpleFont.cpp
    Fonts/TrueTypeFont.cpp
    Fonts/Type0Font.cpp
    Fonts/Type1Font.cpp
    Fonts/Type1FontProgram.cpp
    Fonts/Type3Font.cpp
    Function.cpp
    Interpolation.cpp
    ObjectDerivatives.cpp
    Page.cpp
    Parser.cpp
    Reader.cpp
    Renderer.cpp
    Shading.cpp
    Value.cpp
    )

serenity_lib(LibPDF pdf)
target_link_libraries(LibPDF PRIVATE LibCompress LibCore LibIPC LibGfx LibTextCodec LibCrypto)

include(${SerenityOS_SOURCE_DIR}/Meta/CMake/download_icc_profiles.cmake)
