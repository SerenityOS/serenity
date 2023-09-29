@NetworkSettings::NetworkSettingsWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 20
        }
        fixed_height: 40

        @GUI::ImageWidget {
            fixed_width: 32
            fixed_height: 32
            bitmap: "/res/icons/32x32/network.png"
        }

        @GUI::Label {
            text: "Select adapter:"
            fixed_width: 100
            text_alignment: "CenterLeft"
        }

        @GUI::ComboBox {
            name: "adapters_combobox"
        }
    }

    @GUI::GroupBox {
        title: "Network"
        shrink_to_fit: true
        layout: @GUI::VerticalBoxLayout {
            margins: [10]
        }

        @GUI::CheckBox {
            text: "Enabled"
            name: "enabled_checkbox"
        }

        @GUI::CheckBox {
            text: "Obtain settings automatically (using DHCP)"
            name: "dhcp_checkbox"
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}
            preferred_height: 30

            @GUI::Label {
                text: "IP address:"
                fixed_width: 100
                text_alignment: "CenterLeft"
            }

            @GUI::TextBox {
                name: "ip_address_textbox"
            }

            @GUI::Label {
                text: "/"
                fixed_width: 10
            }

            @GUI::SpinBox {
                name: "cidr_spinbox"
                fixed_width: 50
                min: 1
                max: 32
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}
            preferred_height: 30

            @GUI::Label {
                text: "Default gateway:"
                fixed_width: 100
                text_alignment: "CenterLeft"
            }

            @GUI::TextBox {
                name: "default_gateway_textbox"
            }
        }
    }
}
