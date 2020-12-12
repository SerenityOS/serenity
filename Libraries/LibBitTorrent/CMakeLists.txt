set(SOURCES
    Tracker.cpp
    HTTPTracker.cpp
    MetaInfo.cpp
)

serenity_lib(LibBitTorrent bittorrent)
target_link_libraries(LibBitTorrent LibC)
target_link_libraries(LibBitTorrent LibBencode)
