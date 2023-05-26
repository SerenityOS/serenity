@BrowserSettings::AutoplaySettingsWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::CheckBox {
        name: "allow_autoplay_on_all_websites_checkbox"
        text: "Allow media to automatically play on all websites"
    }

    @GUI::GroupBox {
        title: "Autoplay allowlist"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::ListView {
            name: "allowlist_view"
        }

        @GUI::Widget {
            fixed_height: 32
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Layout::Spacer {}

            @GUI::Button {
                name: "add_website_button"
                fixed_width: 100
                text: "Add Website..."
            }
        }
    }
}
