@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {}
    fill_with_background_color: true

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Frame {
            layout: @GUI::HorizontalBoxLayout {}
            name: "preview_frame"
        }

        @GUI::TabWidget {
            name: "property_tabs"
        }
    }

    @GUI::Widget {
        name: "theme_override_controls"
        layout: @GUI::HorizontalBoxLayout {
            margins: [0, 4]
        }
        fixed_height: 30

        @GUI::Layout::Spacer {}

        @GUI::Button {
            name: "reset"
            text: "Reset to Previous System Theme"
            enabled: false
            fixed_width: 190
        }

        @GUI::Button {
            name: "apply"
            text: "Apply as System Theme"
            enabled: false
            fixed_width: 140
        }
    }
}
