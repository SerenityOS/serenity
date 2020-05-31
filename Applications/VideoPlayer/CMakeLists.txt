set(SOURCES
	main.cpp
    VideoPlayer.cpp
    FrameWidget.cpp
)

serenity_bin(VideoPlayer)
target_link_libraries(VideoPlayer LibAudio LibGUI)
