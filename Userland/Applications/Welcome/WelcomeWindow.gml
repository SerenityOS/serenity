@Welcome::WelcomeWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 4
    }

    @GUI::Widget {
        name: "welcome_banner"
        fixed_height: 30
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 8
        }

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                spacing: 8
            }

            @GUI::Frame {
                name: "tip_frame"
                min_width: 340
                min_height: 170
                fill_with_background_color: true
                background_role: "Base"
                layout: @GUI::HorizontalBoxLayout {
                    margins: [0, 16, 0, 0]
                }

                @GUI::Widget {
                    fixed_width: 60
                    layout: @GUI::VerticalBoxLayout {}

                    @GUI::ImageWidget {
                        fixed_height: 60
                        auto_resize: false
                        bitmap: "/res/icons/32x32/app-welcome.png"
                    }
                }

                @GUI::Widget {
                    layout: @GUI::VerticalBoxLayout {}

                    @GUI::Label {
                        fixed_height: 60
                        name: "did_you_know_label"
                        text: "Did you know..."
                        text_alignment: "CenterLeft"
                        font_size: 12
                        font_weight: "Bold"
                    }

                    @GUI::Label {
                        name: "tip_label"
                        text_alignment: "TopLeft"
                        text_wrapping: "Wrap"
                        font_size: 12
                    }
                }
            }

            @WebView::OutOfProcessWebView {
                name: "web_view"
                min_width: 340
                min_height: 170
                visible: false
            }

            @GUI::CheckBox {
                name: "startup_checkbox"
                text: "Show Welcome the next time SerenityOS starts"
            }
        }

        @GUI::Widget {
            name: "navigation_column"
            fixed_width: 116
            min_height: 170
            layout: @GUI::VerticalBoxLayout {
                spacing: 4
            }

            @GUI::Button {
                name: "new_button"
                text: "What's New"
            }

            @GUI::Button {
                name: "help_button"
                text: "Help Contents"
                icon_from_path: "/res/icons/16x16/book-open.png"
            }

            @GUI::Button {
                name: "next_button"
                text: "Next Tip"
                icon_from_path: "/res/icons/16x16/go-forward.png"
            }

            @GUI::Layout::Spacer {}

            @GUI::HorizontalSeparator {
                fixed_height: 10
            }

            @GUI::Button {
                name: "close_button"
                text: "Close"
            }
        }
    }
}
