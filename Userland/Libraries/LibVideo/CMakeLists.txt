set(SOURCES
    MatroskaDocument.h
    MatroskaReader.cpp
    VP9/Enums.h
    VP9/Symbols.h
)

serenity_lib(LibVideo video)
target_link_libraries(LibVideo LibAudio LibCore LibIPC)
