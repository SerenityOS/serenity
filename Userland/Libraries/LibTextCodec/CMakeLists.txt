set(SOURCES
    Decoder.cpp
    ShiftJISConverter.cpp
)

serenity_lib(LibTextCodec textcodec)
target_link_libraries(LibTextCodec LibC)
