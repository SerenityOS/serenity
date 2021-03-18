@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8, 8, 8, 8]
    }

    @GUI::Widget {
        fixed_height: 30
        layout: @GUI::HorizontalBoxLayout {
            margins: [4, 0, 0, 0]
        }

        @GUI::Label {
            name: "banner_label"
            fixed_width: 251
        }

        @GUI::Widget {
        }
    }

    @GUI::Widget {
        min_height: 160
        layout: @GUI::HorizontalBoxLayout {
        }

        @GUI::Frame {
            name: "tip_frame"
            min_width: 340
            min_height: 160
            layout: @GUI::HorizontalBoxLayout {
                margins: [0, 0, 16, 0]
            }

            @GUI::Widget {
                fixed_width: 60
                layout: @GUI::VerticalBoxLayout {
                }

                @GUI::Label {
                    name: "light_bulb_label"
                    fixed_height: 60
                }

                @GUI::Widget {
                }
            }

            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {
                }

                @GUI::Label {
                    fixed_height: 60
                    name: "did_you_know_label"
                    text: "Did you know..."
                    text_alignment: "CenterLeft"
                }

                @GUI::Label {
                    name: "tip_label"
                    text_alignment: "TopLeft"
                    word_wrap: true
                }
            }
        }

        @Web::OutOfProcessWebView {
            name: "web_view"
            min_width: 340
            min_height: 160
            visible: false
        }

        @GUI::Widget {
            fixed_width: 3
        }

        @GUI::Widget {
            name: "navigation_column"
            fixed_width: 115
            min_height: 160
            layout: @GUI::VerticalBoxLayout {
            }

            @GUI::Button {
                name: "new_button"
                text: "What's New"
            }

            @GUI::Button {
                name: "help_button"
                text: "Help Contents"
            }

            @GUI::Button {
                name: "next_button"
                text: "Next Tip"
            }

            @GUI::Widget {
            }

            @GUI::HorizontalSeparator {
                fixed_height: 2
            }
        }
    }

    @GUI::Widget {
        fixed_height: 5
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {
        }

        @GUI::CheckBox{
            name: "startup_checkbox"
            text: "Show this Welcome Screen next time SerenityOS starts"
            fixed_width: 315
        }

        @GUI::Widget {
        }

        @GUI::Button {
            name: "close_button"
            text: "Close"
            fixed_width: 115
            fixed_height: 24
        }
    }
}
