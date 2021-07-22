@GUI::Widget {
    fixed_height: 24

    @GUI::Label {
        fixed_width: 80
        name: "hash_kind_label"
        text: "<Hash Kind>:"
        tooltip: "Hash Kind"
        text_alignment: "CenterRight"
    }

    layout: @GUI::HorizontalBoxLayout {
        spacing: 6
    }

    @GUI::ImageWidget {
        fixed_width: 24
        fixed_height: 24
        tooltip: "hash status compared to clipboard:\nstatus unknown"
        name: "status_icon"
    }

    @GUI::TextBox {
        fixed_height: 24
        name: "hash_text"
        placeholder: ""
        tooltip: "Hash Value"
        mode: "ReadOnly"
        enabled: false
    }

    @GUI::Button {
        name: "copy_button"
        tooltip: "copy the hash value to the clipboard"
        fixed_width: 24
        fixed_height: 24
        enabled: false
    }
}
