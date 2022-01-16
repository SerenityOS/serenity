set(SOURCES
    Connection.cpp
    Decoder.cpp
    Encoder.cpp
    Message.cpp
    Stub.cpp
    SystemServerTakeover.cpp
)

serenity_lib(LibIPC ipc)
target_link_libraries(LibIPC LibC LibCore)
