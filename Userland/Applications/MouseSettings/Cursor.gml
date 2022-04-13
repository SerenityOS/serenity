@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Cursor speed"
        fixed_height: 106
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                fixed_width: 32
                fixed_height: 32
                icon: "/res/graphics/mouse-cursor-speed.png"
            }

            @GUI::Label {
                text: "The relative speed of the mouse cursor."
                text_alignment: "CenterLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::HorizontalSlider {
                name: "speed_slider"
                min: 0
                max: 100
                value: 50
            }

            @GUI::Label {
                fixed_width: 40
                name: "speed_label"
            }
        }
    }

    @GUI::GroupBox {
        title: "Cursor size"
        fixed_height: 106
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                fixed_width: 32
                fixed_height: 32
                icon: "/res/graphics/mouse-cursor-size.png"
            }

            @GUI::Label {
                text: "The size of the mouse cursor."
                text_alignment: "CenterLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::HorizontalSlider {
                name: "size_slider"
                min: 0
                max: 9
                value: 0
            }

            @GUI::Label {
                fixed_width: 40
                name: "size_label"
            }
        }
    }
}
