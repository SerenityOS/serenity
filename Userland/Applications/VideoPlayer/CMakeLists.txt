serenity_component(
    VideoPlayer
    TARGETS VideoPlayer
    DEPENDS AudioServer
)

stringify_gml(VideoPlayerWindow.gml VideoPlayerWindowGML.h videoplayer_window_gml)

set(SOURCES
    main.cpp
    VideoFrameWidget.cpp
    VideoPlayerWidget.cpp
)

set(GENERATED_SOURCES
    VideoPlayerWindowGML.h
)

serenity_app(VideoPlayer ICON app-video-player)
target_link_libraries(VideoPlayer PRIVATE LibVideo LibAudio LibConfig LibCore LibGfx LibGUI LibMain LibFileSystemAccessClient)
