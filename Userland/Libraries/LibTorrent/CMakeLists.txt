set(SOURCES
         Bencode/Bencode.cpp
        )

serenity_lib(LibTorrent torrent)
target_link_libraries(LibTorrent LibC)
