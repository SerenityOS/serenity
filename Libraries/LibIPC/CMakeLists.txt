set(SOURCES
    Decoder.cpp
    Encoder.cpp
    Endpoint.cpp
    Message.cpp
)

serenity_lib(LibIPC ipc)
target_link_libraries(LibIPC LibC LibCore)
