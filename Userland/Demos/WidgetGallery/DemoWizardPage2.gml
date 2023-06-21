@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [20]
    }

    @GUI::Label {
        text: "Please wait..."
        text_alignment: "TopLeft"
        fixed_height: 32
    }

    @GUI::Progressbar {
        name: "page_2_progressbar"
        fixed_height: 28
    }

    @GUI::Layout::Spacer {}
}
