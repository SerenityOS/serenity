set(SOURCES
    Decoder.cpp
)

serenity_lib(LibTextCodec textcodec)
target_link_libraries(LibTextCodec LibC)
