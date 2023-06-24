@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [8, 12]
        spacing: 10
    }

    @GUI::Widget {
        fixed_height: 34
        layout: @GUI::HorizontalBoxLayout {
            spacing: 20
        }

        @GUI::ImageWidget {
            fixed_width: 32
            fixed_height: 32
            name: "icon"
        }

        @GUI::TextBox {
            text: "file"
            name: "name"
        }
    }

    @GUI::HorizontalSeparator {}

    @GUI::Widget {
        fixed_height: 14
        layout: @GUI::HorizontalBoxLayout {
            spacing: 12
        }

        @GUI::Label {
            text: "Type:"
            text_alignment: "CenterLeft"
            fixed_width: 80
        }

        @GUI::Label {
            name: "type"
            text: "File"
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Widget {
        fixed_height: 14
        layout: @GUI::HorizontalBoxLayout {
            spacing: 12
        }

        @GUI::Label {
            text: "Location:"
            text_alignment: "CenterLeft"
            fixed_width: 80
        }

        @GUI::LinkLabel {
            name: "location"
            text: "/home/anon/file"
            text_alignment: "CenterLeft"
            text_wrapping: "DontWrap"
        }
    }

    @GUI::Widget {
        name: "link_location_widget"
        fixed_height: 14
        layout: @GUI::HorizontalBoxLayout {
            spacing: 12
        }

        @GUI::Label {
            text: "Link location:"
            text_alignment: "CenterLeft"
            fixed_width: 80
        }

        @GUI::LinkLabel {
            name: "link_location"
            text: "/home/anon/file"
            text_alignment: "CenterLeft"
            text_wrapping: "DontWrap"
        }
    }

    @GUI::Widget {
        fixed_height: 28
        layout: @GUI::HorizontalBoxLayout {
            spacing: 12
        }

        @GUI::Label {
            text: "Size:"
            text_alignment: "TopLeft"
            fixed_width: 80
        }

        @GUI::Label {
            name: "size"
            text: "5.9 KiB (6097 bytes)"
            text_alignment: "TopLeft"
        }
    }

    @GUI::Widget {
        fixed_height: 14
        layout: @GUI::HorizontalBoxLayout {
            spacing: 12
        }

        @GUI::Label {
            text: "Owner:"
            text_alignment: "CenterLeft"
            fixed_width: 80
        }

        @GUI::Label {
            name: "owner"
            text: "anon (100)"
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Widget {
        fixed_height: 14
        layout: @GUI::HorizontalBoxLayout {
            spacing: 12
        }

        @GUI::Label {
            text: "Group:"
            text_alignment: "CenterLeft"
            fixed_width: 80
        }

        @GUI::Label {
            name: "group"
            text: "anon (100)"
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Widget {
        fixed_height: 14
        layout: @GUI::HorizontalBoxLayout {
            spacing: 12
        }

        @GUI::Label {
            text: "Created at:"
            text_alignment: "CenterLeft"
            fixed_width: 80
        }

        @GUI::Label {
            name: "created_at"
            text: "2021-07-12 20:33:24"
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Widget {
        fixed_height: 14
        layout: @GUI::HorizontalBoxLayout {
            spacing: 12
        }

        @GUI::Label {
            text: "Last Modified:"
            text_alignment: "CenterLeft"
            fixed_width: 80
        }

        @GUI::Label {
            name: "last_modified"
            text: "2021-07-12 20:33:24"
            text_alignment: "CenterLeft"
        }
    }

    @GUI::HorizontalSeparator {}

    @GUI::Widget {
        fixed_height: 14
        layout: @GUI::HorizontalBoxLayout {
            spacing: 12
        }

        @GUI::Label {
            text: "Owner:"
            text_alignment: "CenterLeft"
            fixed_width: 80
        }

        @GUI::CheckBox {
            name: "owner_read"
            text: "Read"
        }

        @GUI::CheckBox {
            name: "owner_write"
            text: "Write"
        }

        @GUI::CheckBox {
            name: "owner_execute"
            text: "Execute"
        }
    }

    @GUI::Widget {
        fixed_height: 14
        layout: @GUI::HorizontalBoxLayout {
            spacing: 12
        }

        @GUI::Label {
            text: "Group:"
            text_alignment: "CenterLeft"
            fixed_width: 80
        }

        @GUI::CheckBox {
            name: "group_read"
            text: "Read"
        }

        @GUI::CheckBox {
            name: "group_write"
            text: "Write"
        }

        @GUI::CheckBox {
            name: "group_execute"
            text: "Execute"
        }
    }

    @GUI::Widget {
        fixed_height: 14
        layout: @GUI::HorizontalBoxLayout {
            spacing: 12
        }

        @GUI::Label {
            text: "Others:"
            text_alignment: "CenterLeft"
            fixed_width: 80
        }

        @GUI::CheckBox {
            name: "others_read"
            text: "Read"
        }

        @GUI::CheckBox {
            name: "others_write"
            text: "Write"
        }

        @GUI::CheckBox {
            name: "others_execute"
            text: "Execute"
        }
    }
}
