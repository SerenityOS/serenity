serenity_component(
    Bits
    RECOMMENDED
    TARGETS Bits
)

set(SOURCES
    BitsWidget.cpp
    GeneralTorrentInfoWidget.cpp
    PeerListWidget.cpp
    main.cpp
)

serenity_app(Bits ICON hard-disk)
target_link_libraries(Bits PRIVATE LibCore LibGfx LibGUI LibDesktop LibFileSystemAccessClient LibMain LibBitTorrent)
