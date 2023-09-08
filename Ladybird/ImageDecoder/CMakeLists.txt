set(IMAGE_DECODER_SOURCE_DIR ${SERENITY_SOURCE_DIR}/Userland/Services/ImageDecoder)

set(IMAGE_DECODER_SOURCES
    ${IMAGE_DECODER_SOURCE_DIR}/ConnectionFromClient.cpp
    main.cpp
)

add_executable(ImageDecoder ${IMAGE_DECODER_SOURCES})

target_include_directories(ImageDecoder PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Services/)
target_include_directories(ImageDecoder PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/..)
target_link_libraries(ImageDecoder PRIVATE LibCore LibGfx LibIPC LibImageDecoderClient LibMain)
