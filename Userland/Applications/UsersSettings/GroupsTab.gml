@UsersSettings::GroupsTab {
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 6
    }
    fill_with_background_color: true

    @GUI::Widget {
        fixed_height: 120
        layout: @GUI::VerticalBoxLayout {
            spacing: 4
        }

        @GUI::ListView {
            name: "groups_list"
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
                name: "add_button"
                text: "Add Group..."
                fixed_width: 100
            }

            @GUI::Button {
                name: "delete_button"
                text: "Delete Group"
                fixed_width: 100
            }
        }
    }

    @GUI::HorizontalSeparator {}

    @GUI::Widget {
        name: "details_container"
        layout: @GUI::VerticalBoxLayout {}
    }
}
