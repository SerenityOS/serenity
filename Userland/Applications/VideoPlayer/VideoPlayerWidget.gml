@VideoPlayer::VideoPlayerWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @VideoPlayer::VideoFrameWidget {
        name: "video_frame"
        auto_resize: false
    }

    @GUI::Widget {
        name: "bottom_container"
        max_height: 50
        layout: @GUI::VerticalBoxLayout {}

        @GUI::HorizontalSlider {
            name: "seek_slider"
            fixed_height: 20
            enabled: false
            jump_to_cursor: true
        }

        @GUI::ToolbarContainer {
            @GUI::Toolbar {
                name: "toolbar"

                @GUI::Button {
                    name: "playback"
                    icon_from_path: "/res/icons/16x16/play.png"
                    fixed_width: 24
                    button_style: "Coolbar"
                }

                @GUI::VerticalSeparator {}

                @GUI::Label {
                    name: "timestamp"
                    autosize: true
                }

                @GUI::Layout::Spacer {}

                @GUI::Button {
                    name: "sizing"
                    icon_from_path: "/res/icons/16x16/fit-image-to-view.png"
                    fixed_width: 24
                    button_style: "Coolbar"
                }

                @GUI::VerticalSeparator {}

                @GUI::HorizontalSlider {
                    name: "volume_slider"
                    min: 0
                    max: 100
                    fixed_width: 100
                }

                @GUI::VerticalSeparator {}

                @GUI::Button {
                    name: "fullscreen"
                    icon_from_path: "/res/icons/16x16/fullscreen.png"
                    fixed_width: 24
                    button_style: "Coolbar"
                }
            }
        }
    }
}
