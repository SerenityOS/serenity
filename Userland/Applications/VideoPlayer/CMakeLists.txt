serenity_component(
    VideoPlayer
    TARGETS VideoPlayer
    DEPENDS AudioServer
)

set(SOURCES
    main.cpp
    VideoFrameWidget.cpp
    VideoPlayerWidget.cpp
)

serenity_app(VideoPlayer ICON app-video-player)
target_link_libraries(VideoPlayer PRIVATE LibVideo LibAudio LibCore LibGfx LibGUI LibMain)
