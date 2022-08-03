@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::Widget {
        preferred_height: "fit"
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Taskbar Mode:"
            text_alignment: "CenterLeft"
        }

        @GUI::RadioButton {
            name: "classic"
            text: "Classic"
        }

        @GUI::RadioButton {
            name: "modern"
            text: "Informative"
        }
    }

    @GUI::Widget {
        fixed_height: 76
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            fixed_height: 32
            fixed_width: 32
            icon: "/res/icons/32x32/app-welcome.png"
        }

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                margins: [6]
            }

            @GUI::Label {
                text: "Informative mode replaces the Serenity menu with the Dashboard app."
                text_alignment: "TopLeft"
                word_wrap: true
            }

            @GUI::Label {
                text: "You can still access the classic Serenity menu through a secondary click."
                text_alignment: "TopLeft"
                word_wrap: true
            }
        }
    }
}
