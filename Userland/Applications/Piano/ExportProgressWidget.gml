@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Label {
        name: "export_message"
        text_alignment: "Center"
        // FIXME: Change to dynamic width once that works.
        min_width: 300
        preferred_height: "fit"
    }

    @GUI::HorizontalProgressbar {
        name: "progress_bar"
        min: 0
        max: 100
        preferred_width: "grow"
        min_height: 40
    }
}
