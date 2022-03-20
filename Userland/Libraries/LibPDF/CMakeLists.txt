set(SOURCES
    ColorSpace.cpp
    CommonNames.cpp
    Document.cpp
    Encoding.cpp
    Encryption.cpp
    Filter.cpp
    Fonts.cpp
    ObjectDerivatives.cpp
    Parser.cpp
    Renderer.cpp
    Value.cpp
    )

serenity_lib(LibPDF pdf)
target_link_libraries(LibPDF LibC LibCore LibIPC LibGfx LibTextCodec LibCrypto)
