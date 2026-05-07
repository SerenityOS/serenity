@UsersSettings::GroupDetailsWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 4
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Group Name:"
            text_alignment: "CenterLeft"
            fixed_width: 120
        }

        @GUI::TextBox {
            name: "group_name_textbox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "GID:"
            text_alignment: "CenterLeft"
            fixed_width: 120
        }

        @GUI::TextBox {
            name: "gid_textbox"
            mode: "DisplayOnly"
        }
    }

    @GUI::Label {
        text: "Group Members:"
        text_alignment: "CenterLeft"
        fixed_height: 16
    }

    @GUI::ListView {
        name: "members_list"
        should_hide_unnecessary_scrollbars: true
        alternating_row_colors: false
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::Layout::Spacer {}

        @GUI::Button {
            name: "add_member_button"
            text: "Add Member..."
            fixed_width: 110
        }

        @GUI::Button {
            name: "remove_member_button"
            text: "Remove Member"
            fixed_width: 110
        }
    }
}
