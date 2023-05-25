set(SOURCES
    Connection.cpp
    Decoder.cpp
    Encoder.cpp
    TrafficDump.cpp
)

serenity_lib(LibIPC ipc)
target_link_libraries(LibIPC PRIVATE LibCore)
