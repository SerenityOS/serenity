@GUI::Widget {
    fill_with_background_color: true
    visible: false
    layout: @GUI::HorizontalBoxLayout {
        margins: [4]
    }

    @GUI::TextBox {
        name: "search_textbox"
        max_width: 250
        preferred_width: "grow"
        placeholder: "Find"
    }

    @GUI::Widget {
        preferred_width: "shrink"
        layout: @GUI::HorizontalBoxLayout {
            spacing: 0
        }

        @GUI::Button {
            name: "next_button"
            icon: "/res/icons/16x16/go-down.png"
            fixed_width: 18
            button_style: "Coolbar"
            focus_policy: "NoFocus"
        }

        @GUI::Button {
            name: "previous_button"
            icon: "/res/icons/16x16/go-up.png"
            fixed_width: 18
            button_style: "Coolbar"
            focus_policy: "NoFocus"
        }
    }

    @GUI::Label {
        name: "index_label"
        text_alignment: "CenterLeft"
    }

    @GUI::Layout::Spacer {}

    @GUI::Widget {
        preferred_width: "shrink"
        layout: @GUI::HorizontalBoxLayout {
            spacing: 0
        }

        @GUI::Button {
            name: "wrap_search_button"
            fixed_width: 24
            icon: "/res/icons/16x16/reload.png"
            tooltip: "Wrap Search"
            checkable: true
            checked: true
            button_style: "Coolbar"
            focus_policy: "NoFocus"
        }

        @GUI::Button {
            name: "match_case_button"
            fixed_width: 24
            icon: "/res/icons/16x16/app-font-editor.png"
            tooltip: "Match Case"
            checkable: true
            button_style: "Coolbar"
            focus_policy: "NoFocus"
        }
    }

    @GUI::VerticalSeparator {}

    @GUI::Button {
        name: "close_button"
        fixed_size: [15, 16]
        button_style: "Coolbar"
        focus_policy: "NoFocus"
    }
}
