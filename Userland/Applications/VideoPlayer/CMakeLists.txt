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

serenity_bin(VideoPlayer)
target_link_libraries(VideoPlayer LibVideo LibAudio LibGUI LibMain)
