set(SOURCES
    Cipher.cpp
    DataTypes.cpp
    IdentificationString.cpp
    KeyExchangeData.cpp
    Peer.cpp
    Session.cpp
)

serenity_lib(LibSSH ssh)
target_link_libraries(LibSSH PRIVATE LibCore LibCrypto)
