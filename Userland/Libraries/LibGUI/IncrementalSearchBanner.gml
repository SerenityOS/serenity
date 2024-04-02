@GUI::IncrementalSearchBanner {
    fill_with_background_color: true
    visible: false
    layout: @GUI::HorizontalBoxLayout {
        margins: [4]
    }

    @GUI::TextBox {
        name: "incremental_search_banner_search_textbox"
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
            name: "incremental_search_banner_previous_button"
            icon_from_path: "/res/icons/16x16/go-up.png"
            fixed_width: 18
            button_style: "Coolbar"
            focus_policy: "NoFocus"
        }

        @GUI::Button {
            name: "incremental_search_banner_next_button"
            icon_from_path: "/res/icons/16x16/go-down.png"
            fixed_width: 18
            button_style: "Coolbar"
            focus_policy: "NoFocus"
        }
    }

    @GUI::Label {
        name: "incremental_search_banner_index_label"
        text_alignment: "CenterLeft"
    }

    @GUI::Layout::Spacer {}

    @GUI::Widget {
        preferred_width: "shrink"
        layout: @GUI::HorizontalBoxLayout {
            spacing: 0
        }

        @GUI::Button {
            name: "incremental_search_banner_wrap_search_button"
            fixed_width: 24
            icon_from_path: "/res/icons/16x16/reload.png"
            tooltip: "Wrap Search"
            checkable: true
            checked: true
            button_style: "Coolbar"
            focus_policy: "NoFocus"
        }

        @GUI::Button {
            name: "incremental_search_banner_match_case_button"
            fixed_width: 24
            icon_from_path: "/res/icons/16x16/app-font-editor.png"
            tooltip: "Match Case"
            checkable: true
            button_style: "Coolbar"
            focus_policy: "NoFocus"
        }
    }

    @GUI::VerticalSeparator {}

    @GUI::Button {
        name: "incremental_search_banner_close_button"
        fixed_size: [15, 16]
        button_style: "Coolbar"
        focus_policy: "NoFocus"
    }
}
