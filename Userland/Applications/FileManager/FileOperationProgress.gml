@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        fixed_height: 32
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::ImageWidget {
            name: "source_folder_icon"
        }

        @GUI::ImageWidget {
            name: "file_copy_animation"
        }

        @GUI::ImageWidget {
            name: "destination_folder_icon"
        }
    }

    @GUI::Label {
        text: "Copying files..."
        text_alignment: "CenterLeft"
        font_weight: "Bold"
        fixed_height: 32
        name: "files_copied_label"
    }

    @GUI::HorizontalSeparator {
        fixed_height: 2
    }

    @GUI::Widget {
        fixed_height: 22
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Copying: "
            font_weight: "Bold"
            text_alignment: "CenterLeft"
            fixed_width: 80
            name: "current_file_action_label"
        }

        @GUI::Label {
            name: "current_file_label"
            text: "Placeholder"
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Widget {
        fixed_height: 22
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Time left: "
            font_weight: "Bold"
            text_alignment: "CenterLeft"
            fixed_width: 80
        }

        @GUI::Label {
            name: "estimated_time_label"
            text: "Estimating..."
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Progressbar {
        fixed_height: 22
        name: "overall_progressbar"
        min: 0
    }

    @GUI::Widget {
        fixed_height: 2
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Layout::Spacer {}

        @GUI::Button {
            text: "Cancel"
            name: "button"
            fixed_width: 80
        }
    }
}
