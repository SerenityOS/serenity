set(SOURCES
    Connection.cpp
    Decoder.cpp
    Encoder.cpp
    Message.cpp
)

serenity_lib(LibIPC ipc)
target_link_libraries(LibIPC PRIVATE LibCore LibURL)

serenity_static_lib(LibIPCStatic)
target_link_libraries(LibIPCStatic PRIVATE LibCoreStatic LibURLStatic)
