@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [5]
    }

    @GUI::Widget {
        fixed_height: 44
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        @GUI::ImageWidget {
            name: "icon"
        }

        @GUI::Label {
            name: "description"
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Widget {
        fixed_height: 18
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Executable path:"
            text_alignment: "CenterLeft"
            fixed_width: 90
        }

        @GUI::LinkLabel {
            name: "executable_link"
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Widget {
        fixed_height: 18
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Coredump path:"
            text_alignment: "CenterLeft"
            fixed_width: 90
        }

        @GUI::LinkLabel {
            name: "coredump_link"
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Widget {
        fixed_height: 18
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Arguments:"
            text_alignment: "CenterLeft"
            fixed_width: 90
        }

        @GUI::Label {
            name: "arguments_label"
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Progressbar {
        name: "progressbar"
        text: "Generating crash report: "
    }

    @GUI::TabWidget {
        name: "tab_widget"
        visible: false
    }

    @GUI::Widget {
        fixed_height: 32
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::DialogButton {
            name: "debug_button"
            text: "Debug in Hack Studio"
            preferred_width: 160
        }

        @GUI::DialogButton {
            name: "save_backtrace_button"
            text: "Save Backtrace"
            preferred_width: 160
        }

        @GUI::Layout::Spacer {}

        @GUI::DialogButton {
            name: "close_button"
            text: "Close"
        }
    }
}
