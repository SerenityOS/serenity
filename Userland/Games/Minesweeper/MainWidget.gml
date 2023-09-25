@Minesweeper::MainWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 0
    }

    @GUI::HorizontalSeparator {
        name: "separator"
        fixed_height: 2
    }

    @GUI::Widget {
        name: "container"
        fixed_height: 36
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Layout::Spacer {}

        @GUI::ImageWidget {
            name: "flag_image"
            bitmap: "/res/graphics/minesweeper/flag.png"
        }

        @GUI::Label {
            name: "flag_label"
            autosize: true
        }

        @GUI::Layout::Spacer {}

        @GUI::Button {
            name: "face_button"
            fixed_size: [36, 36]
            focus_policy: "TabFocus"
            button_style: "Coolbar"
        }

        @GUI::Layout::Spacer {}

        @GUI::ImageWidget {
            name: "time_image"
            bitmap: "/res/graphics/minesweeper/timer.png"
        }

        @GUI::Label {
            name: "time_label"
            autosize: true
        }

        @GUI::Layout::Spacer {}
    }
}
