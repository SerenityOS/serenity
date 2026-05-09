@GUI::Widget {
    layout: @GUI::HorizontalBoxLayout {
        margins: [4]
        spacing: 4
    }
    preferred_height: "shrink"

    @GUI::Button {
        name: "close_button"
        fixed_width: 16
        fixed_height: 16
        button_style: "Coolbar"
    }

    @GUI::TextBox {
        name: "search_textbox"
        placeholder: "Find in page..."
        fixed_width: 200
    }

    @GUI::Button {
        name: "previous_button"
        fixed_width: 24
        button_style: "Coolbar"
    }

    @GUI::Button {
        name: "next_button"
        fixed_width: 24
        button_style: "Coolbar"
    }

    @GUI::CheckBox {
        name: "match_case_checkbox"
        text: "Match Case"
    }

    @GUI::Label {
        name: "result_label"
        text: ""
        fixed_width: 100
    }
}
