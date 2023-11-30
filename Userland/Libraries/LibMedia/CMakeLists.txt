set(SOURCES
    Color/ColorConverter.cpp
    Color/ColorPrimaries.cpp
    Color/TransferCharacteristics.cpp
    Containers/Matroska/MatroskaDemuxer.cpp
    Containers/Matroska/Reader.cpp
    PlaybackManager.cpp
    VideoFrame.cpp
    VP9/Decoder.cpp
    VP9/Parser.cpp
    VP9/ProbabilityTables.cpp
    VP9/SyntaxElementCounter.cpp
    VP9/TreeParser.cpp
    ImageFormats/BMPLoader.cpp
    ImageFormats/BMPWriter.cpp
    ImageFormats/BooleanDecoder.cpp
    ImageFormats/TIFFLoader.cpp
    ImageFormats/DDSLoader.cpp
    ImageFormats/GIFLoader.cpp
    ImageFormats/ICOLoader.cpp
    ImageFormats/ILBMLoader.cpp
    ImageFormats/ImageDecoder.cpp
    ImageFormats/ISOBMFF/Boxes.cpp
    ImageFormats/ISOBMFF/Reader.cpp
    ImageFormats/JPEGLoader.cpp
    ImageFormats/JPEGXLLoader.cpp
    ImageFormats/JPEGWriter.cpp
    ImageFormats/PBMLoader.cpp
    ImageFormats/PGMLoader.cpp
    ImageFormats/PNGLoader.cpp
    ImageFormats/PNGWriter.cpp
    ImageFormats/PortableFormatWriter.cpp
    ImageFormats/PPMLoader.cpp
    ImageFormats/QOILoader.cpp
    ImageFormats/QOIWriter.cpp
    ImageFormats/TGALoader.cpp
    ImageFormats/TinyVGLoader.cpp
    ImageFormats/WebPLoader.cpp
    ImageFormats/WebPLoaderLossless.cpp
    ImageFormats/WebPLoaderLossy.cpp
)

serenity_lib(LibMedia media)
target_link_libraries(LibMedia PRIVATE LibAudio LibCompress LibCore LibCrypto LibGfx LibThreading)

set(generated_sources TIFFMetadata.h TIFFTagHandler.cpp)
list(TRANSFORM generated_sources PREPEND "ImageFormats/")

find_package(Python COMPONENTS Interpreter REQUIRED)

add_custom_command(
        OUTPUT  ${generated_sources}
        COMMAND ${Python_EXECUTABLE} "${CMAKE_CURRENT_SOURCE_DIR}/TIFFGenerator.py" -o "${CMAKE_CURRENT_BINARY_DIR}/ImageFormats"
        DEPENDS "TIFFGenerator.py"
        VERBATIM
)
target_sources(LibMedia PRIVATE ${generated_sources})
add_custom_target(generate_tiff_files_handler DEPENDS ${generated_sources})
add_dependencies(all_generated generate_tiff_files_handler)
add_dependencies(LibMedia generate_tiff_files_handler)

list(TRANSFORM generated_sources PREPEND "${CMAKE_CURRENT_BINARY_DIR}/")
install(FILES ${generated_sources} DESTINATION "${CMAKE_INSTALL_INCLUDEDIR}/LibMedia/ImageFormats")
