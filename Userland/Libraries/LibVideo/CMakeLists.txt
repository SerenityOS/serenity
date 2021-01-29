set(SOURCES
    MatroskaDocument.h
    MatroskaReader.cpp
    VPX/BitStream.cpp
    VPX/Constants.h
    VPX/Enums.h
    VPX/LookupTables.h
    VPX/ProbabilityTables.cpp
    VPX/SyntaxElementCounter.cpp
    VPX/VP9Decoder.cpp
)

serenity_lib(LibVideo video)
target_link_libraries(LibVideo LibAudio LibCore LibIPC)
