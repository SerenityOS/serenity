set(SOURCES
    DataTypes.cpp
    IdentificationString.cpp
    KeyExchangeData.cpp
    Peer.cpp
)

serenity_lib(LibSSH ssh)
target_link_libraries(LibSSH PRIVATE LibCrypto)
