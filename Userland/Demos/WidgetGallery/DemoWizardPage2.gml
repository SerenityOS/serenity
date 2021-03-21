@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [20, 20, 20, 20]
    }

    @GUI::Label {
        text: "Please wait..."
        text_alignment: "TopLeft"
        fixed_height: 32
    }

    @GUI::ProgressBar {
        name: "page_2_progress_bar"
        fixed_height: 28
    }

    // Spacer
    @GUI::Widget
}
