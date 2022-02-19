@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::CheckBox {
        name: "enable_content_filtering_checkbox"
        text: "Enable content filtering"
    }

    @GUI::GroupBox {
        title: "Domain list"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::ListView {
            name: "domain_list_view"
        }

        @GUI::Widget {
            fixed_height: 32
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Widget {}

            @GUI::Button {
                name: "add_new_domain_button"
                fixed_width: 100
                text: "Add new domain"
            }
        }
    }
}
