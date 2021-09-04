@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8, 8, 8, 8]
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::HorizontalBoxLayout

        @GUI::Label {
            name: "score_label"
            text: "Score: 0"
            text_alignment: "TopLeft"
            fixed_height: 12
        }
        @GUI::Label {
            name: "best_score_label"
            text: "Best score: 0"
            text_alignment: "TopCenter"
            fixed_height: 12
        }
        @GUI::Label {
            name: "lives_label"
            text: "Lives: 5"
            text_alignment: "TopRight"
            fixed_height: 12
        }

    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {
            alignment: "Center"
        }

        @GUI::Label {
            name: "prompt_label"
            fixed_height: 50
            text: "Sample prompt"
            text_alignment: "Center"
            word_wrap: true
            font_size: 25
        }
        @GUI::Widget {
            name: "choice_buttons"
            layout: @GUI::VerticalBoxLayout

            @GUI::Button {
                text: "Sample text"
            }
            @GUI::Button {
                text: "Sample text"
            }
            @GUI::Button {
                text: "Sample text"
            }
        }
    }
}
