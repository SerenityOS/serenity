set(SOURCES
    Announcer.cpp
    Bencode/BDecoder.cpp
    Bencode/BEncoder.cpp
    BitField.cpp
    Engine.cpp
    Files.cpp
    Message.cpp
    MetaInfo.cpp
    Net/Connection.cpp
    Net/ConnectionManager.cpp
    Peer.cpp
    PeerSession.cpp
    PieceHeap.cpp
    Torrent.cpp
    TorrentDataFileMap.cpp
)

serenity_lib(LibBitTorrent bittorrent)
target_link_libraries(LibBitTorrent PRIVATE LibCore LibCrypto LibThreading LibProtocol LibHTTP)
