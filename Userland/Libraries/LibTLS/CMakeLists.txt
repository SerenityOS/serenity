set(SOURCES
    ClientHandshake.cpp
    Exchange.cpp
    Handshake.cpp
    Record.cpp
    Socket.cpp
    TLSv12.cpp
)

serenity_lib(LibTLS tls)
target_link_libraries(LibTLS LibCore LibCrypto)
