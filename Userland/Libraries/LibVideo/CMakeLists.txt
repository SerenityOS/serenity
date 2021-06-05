set(SOURCES
    MatroskaDocument.h
    MatroskaReader.cpp
)

serenity_lib(LibVideo video)
target_link_libraries(LibVideo LibAudio LibCore LibIPC)
