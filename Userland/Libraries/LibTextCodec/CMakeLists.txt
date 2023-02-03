set(SOURCES
    Decoder.cpp
)

serenity_lib(LibTextCodec textcodec)

if (NOT BUILD_LAGOM AND NOT BUILD_LAGOM_TOOLS)
    serenity_lib_static(LibTextCodecStatic textcodec)
endif()
