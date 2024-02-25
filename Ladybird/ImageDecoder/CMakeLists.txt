set(IMAGE_DECODER_SOURCE_DIR ${SERENITY_SOURCE_DIR}/Userland/Services/ImageDecoder)

set(CMAKE_AUTOMOC OFF)
set(CMAKE_AUTORCC OFF)
set(CMAKE_AUTOUIC OFF)

set(IMAGE_DECODER_SOURCES
    ${IMAGE_DECODER_SOURCE_DIR}/ConnectionFromClient.cpp
)

if (ANDROID)
    add_library(imagedecoder_svc SHARED
            ${IMAGE_DECODER_SOURCES}
            ../Android/src/main/cpp/ImageDecoderService.cpp
            ../Android/src/main/cpp/LadybirdServiceBaseJNI.cpp
            ../Utilities.cpp
            )
    set_target_properties(imagedecoder_svc PROPERTIES OUTPUT_NAME "imagedecoder")
else()
    add_library(imagedecoder_svc STATIC ${IMAGE_DECODER_SOURCES})
endif()

add_executable(ImageDecoder main.cpp)
target_link_libraries(ImageDecoder PRIVATE imagedecoder_svc LibMain)

target_include_directories(imagedecoder_svc PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Services/)
target_include_directories(imagedecoder_svc PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/..)
target_link_libraries(imagedecoder_svc PRIVATE LibCore LibGfx LibIPC LibImageDecoderClient LibMain)
set_helper_process_properties(ImageDecoder)
