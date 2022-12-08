@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::Label {
        text: "These settings allow you to override the footer settings of all presentations."
        text_alignment: "TopLeft"
        // FIXME: Use dynamic sizing once supported.
        min_height: 50
    }

    @GUI::CheckBox {
        name: "override_footer"
        text: "Override presentation footers"
        checked: false
    }

    @GUI::GroupBox {
        title: "Footer overrides"
        enabled: false
        layout: @GUI::VerticalBoxLayout {
            margins: [4]
            spacing: 5
        }

        @GUI::CheckBox {
            name: "enable_footer"
            text: "Enable all presentation footers"
            checked: true
        }

        @GUI::Label {
            text: "Set footer\nAvailable placeholders: {presentation_title}, {slide_title}, {author}, {slides_total}, {frames_total}, {date}, {slide_number}, {slide_frame_number} (Number of frame within slide), {slide_frames_total}, {frame_number} (All frames on all slides)"
            text_alignment: "TopLeft"
            // FIXME: Use dynamic sizing once supported.
            min_height: 70
        }

        @GUI::TextEditor {
            name: "footer_text"
            preferred_height: "fit"
            width: "fit"
            placeholder: "{presentation_title} - {slide_title}"
        }
    }
}
