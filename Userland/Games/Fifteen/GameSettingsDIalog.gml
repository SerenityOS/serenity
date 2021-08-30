@GUI::Frame {
    layout: @GUI::VerticalBoxLayout {
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 2
            margins: [4]
        }
        fixed_height: 22

        @GUI::Label {
            text: "Columns:"
            autosize: true
        }
        @GUI::SpinBox {
            name: "NumberOfColumns"
            fixed_width: 64
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 2
            margins: [4]
        }
        fixed_height: 22

        @GUI::Label {
            text: "Rows:   "
            autosize: true
        }
        @GUI::SpinBox {
            name: "NumberOfRows"
            fixed_width: 64
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout
        fixed_height: 22

        @GUI::Button {
            name: "OkButton"
            text: "OK"
        }

        @GUI::Button {
            name: "CancelButton"
            text: "Cancel"
        }
    }
}