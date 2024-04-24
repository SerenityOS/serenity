@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Label {
        text: "Templates:"
        text_alignment: "CenterLeft"
        max_height: 20
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {}
        name: "icon_view_container"
    }

    @GUI::Label {
        name: "description_label"
        text_alignment: "CenterLeft"
        frame_style: "SunkenContainer"
        max_height: 24
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        max_height: 24

        @GUI::Label {
            text: "Name:"
            text_alignment: "CenterLeft"
            max_width: 75
        }

        @GUI::TextBox {
            name: "name_input"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        max_height: 24

        @GUI::Label {
            text: "Create in:"
            text_alignment: "CenterLeft"
            max_width: 75
        }

        @GUI::TextBox {
            name: "create_in_input"
            text: "/home/anon/Source"
        }

        @GUI::Button {
            name: "browse_button"
            text: "Browse"
            max_width: 75
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        max_height: 24

        @GUI::Label {
            text: "Full path:"
            text_alignment: "CenterLeft"
            max_width: 75
        }

        @GUI::Label {
            name: "full_path_label"
            text_alignment: "CenterLeft"
            text: ""
            frame_style: "SunkenContainer"
            max_height: 22
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        max_height: 24

        @GUI::Layout::Spacer {}

        @GUI::Button {
            name: "ok_button"
            text: "OK"
            max_width: 75
        }

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
            max_width: 75
        }
    }
}
