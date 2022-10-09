set(SOURCES
    MatroskaReader.cpp
    VP9/BitStream.cpp
    VP9/Decoder.cpp
    VP9/MV.cpp
    VP9/Parser.cpp
    VP9/ProbabilityTables.cpp
    VP9/SyntaxElementCounter.cpp
    VP9/TreeParser.cpp
    VP9/Utilities.cpp
)

serenity_lib(LibVideo video)
target_link_libraries(LibVideo LibAudio LibCore LibIPC)
