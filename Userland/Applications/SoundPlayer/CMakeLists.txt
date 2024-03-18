serenity_component(
    SoundPlayer
    RECOMMENDED
    TARGETS SoundPlayer
    DEPENDS AudioServer
)

set(SOURCES
    AlbumCoverVisualizationWidget.cpp
    BarsVisualizationWidget.cpp
    M3UParser.cpp
    PlaybackManager.cpp
    Player.cpp
    Playlist.cpp
    PlaylistWidget.cpp
    SampleWidget.cpp
    SoundPlayerWidget.cpp
    main.cpp
)

serenity_app(SoundPlayer ICON app-sound-player)
target_link_libraries(SoundPlayer PRIVATE LibAudio LibConfig LibCore LibFileSystem LibDSP LibGfx LibGUI LibIPC LibMain LibThreading LibImageDecoderClient LibURL)
