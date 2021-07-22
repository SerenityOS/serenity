@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [6, 6, 6, 6]
        spacing: 8
    }

    @GUI::Widget {
        name: "spacer"
    }

    @GUI::Button {
        fixed_height: 22
        text: "Calculate Hashes"
        tooltip: "calculate the hashes of this file in its current state"
        name: "calculate_button"
    }
}
