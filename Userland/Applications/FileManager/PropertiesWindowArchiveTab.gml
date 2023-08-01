@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 12
    }

    @GUI::GroupBox {
        title: "Archive"
        preferred_height: "shrink"
        layout: @GUI::VerticalBoxLayout {
            margins: [12, 8, 0]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Format:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "archive_format"
                text: "ZIP"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Files:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "archive_file_count"
                text: "42"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Directories:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "archive_directory_count"
                text: "7"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Uncompressed:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "archive_uncompressed_size"
                text: "3.3 MiB"
                text_alignment: "TopLeft"
            }
        }
    }
}
