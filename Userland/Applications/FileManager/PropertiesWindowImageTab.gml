@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 12
    }

    @GUI::GroupBox {
        title: "Image"
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
                text: "Type:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "image_type"
                text: "JPEG"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Size:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "image_size"
                text: "1920 x 1080"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Animation:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "image_animation"
                text: "Loop indefinitely (12 frames)"
                text_alignment: "TopLeft"
                text_wrapping: "DontWrap"
            }
        }

        @GUI::Widget {
            name: "image_has_icc_line"
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "ICC profile:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "image_has_icc_profile"
                text: "See below"
                text_alignment: "TopLeft"
            }
        }
    }

    @GUI::GroupBox {
        name: "image_icc_group"
        title: "ICC Profile"
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
                text: "Profile:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "image_icc_profile"
                text: "e-sRGB"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Copyright:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "image_icc_copyright"
                text: "(c) 1999 Adobe Systems Inc."
                text_alignment: "TopLeft"
                text_wrapping: "DontWrap"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Color space:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "image_icc_color_space"
                text: "RGB"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Device class:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "image_icc_device_class"
                text: "DisplayDevice"
                text_alignment: "TopLeft"
            }
        }
    }

    @GUI::GroupBox {
        name: "image_basic_metadata"
        title: "Basic Metadata"
        preferred_height: "shrink"
        visible: false
        layout: @GUI::VerticalBoxLayout {
            margins: [12, 8, 0]
            spacing: 2
        }
    }

    @GUI::GroupBox {
        name: "image_gps"
        title: "GPS Location"
        preferred_height: 200
        visible: false
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }
    }
}
