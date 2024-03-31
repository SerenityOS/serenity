@MailSettings::MailSettingsWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::GroupBox {
        title: "Server settings"
        fixed_height: 170
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::ImageWidget {
                fixed_width: 32
                fixed_height: 32
                bitmap: "/res/graphics/mail-server-settings.png"
            }

            @GUI::Label {
                text: "These settings specify the mail server from which you would like to fetch your mail."
                text_alignment: "CenterLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::Label {
                text: "Server address:"
                fixed_width: 80
                name: "server_label"
                text_alignment: "CenterLeft"
            }

            @GUI::TextBox {
                name: "server_input"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::Label {
                text: "Server port:"
                fixed_width: 80
                name: "port_label"
                text_alignment: "CenterLeft"
            }

            @GUI::ComboBox {
                name: "port_input"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::CheckBox {
                name: "tls_input"
                text: "Use TLS"
            }
        }
    }

    @GUI::GroupBox {
        title: "User settings"
        fixed_height: 110
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::ImageWidget {
                fixed_width: 32
                fixed_height: 32
                bitmap: "/res/graphics/mail-user-settings.png"
            }

            @GUI::Label {
                text: "These settings specify the credentials which will be used to send and receive mail, and how you identify yourself to recipients."
                text_alignment: "CenterLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::Label {
                autosize: true
                text: "Email address:"
                fixed_width: 80
                text_alignment: "CenterLeft"
            }

            @GUI::TextBox {
                name: "email_input"
            }
        }
    }
}
