@UsersSettings::GroupAddDialog {
    fixed_width: 260
    fixed_height: 64
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Group Name:"
            text_alignment: "CenterLeft"
            fixed_width: 90
        }

        @GUI::TextBox {
            name: "group_name_textbox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Layout::Spacer {}

        @GUI::DialogButton {
            name: "ok_button"
            text: "Add"
            fixed_width: 75
        }

        @GUI::DialogButton {
            name: "cancel_button"
            text: "Cancel"
            fixed_width: 75
        }
    }
}
