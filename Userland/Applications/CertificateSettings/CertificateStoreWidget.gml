@CertificateSettings::CertificateStoreWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::GroupBox {
        title: "Trusted Root Certification Authorities"
        fixed_height: 500
        fixed_width: 465
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::TableView {
            name: "root_ca_tableview"
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 6
            }
            preferred_height: "fit"

            @GUI::Button {
                name: "import_button"
                text: "Import..."
                fixed_width: 80
            }

            @GUI::Button {
                name: "export_button"
                text: "Export..."
                fixed_width: 80
                enabled: false
            }
        }
    }
}
