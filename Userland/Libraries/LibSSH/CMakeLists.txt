set(SOURCES
    Cipher.cpp
    DataTypes.cpp
    IdentificationString.cpp
    KeyExchangeData.cpp
    Peer.cpp
    Session.cpp
    SFTP/Peer.cpp
    SFTP/Server.cpp
)

serenity_lib(LibSSH ssh)
target_link_libraries(LibSSH PRIVATE LibCrypto)
