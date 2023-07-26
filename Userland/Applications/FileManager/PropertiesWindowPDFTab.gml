@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 12
    }

    @GUI::GroupBox {
        title: "PDF"
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
                text: "Version:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "pdf_version"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Page count:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "pdf_page_count"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Title:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "pdf_title"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Author:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "pdf_author"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Subject:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "pdf_subject"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Keywords:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "pdf_keywords"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Creator:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "pdf_creator"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Producer:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "pdf_producer"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Created:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "pdf_creation_date"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Modified:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "pdf_modification_date"
                text_alignment: "TopLeft"
            }
        }
    }
}
