serenity_component(
    VideoPlayer
    TARGETS VideoPlayer
    DEPENDS AudioServer
)

compile_gml(VideoPlayerWidget.gml VideoPlayerWidgetGML.cpp)

set(SOURCES
    main.cpp
    VideoPlayerWidgetGML.cpp
    VideoFrameWidget.cpp
    VideoPlayerWidget.cpp
)

serenity_app(VideoPlayer ICON app-video-player)
target_link_libraries(VideoPlayer PRIVATE LibAudio LibConfig LibCore LibGfx LibGUI LibMain LibMedia LibFileSystemAccessClient LibURL)
