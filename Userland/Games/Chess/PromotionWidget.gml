@Chess::PromotionWidget {
    fixed_height: 70
    fill_with_background_color: true
    layout: @GUI::HorizontalBoxLayout {}

    @GUI::Button {
        fixed_width: 70
        fixed_height: 70
        name: "queen_button"
    }

    @GUI::Button {
        fixed_width: 70
        fixed_height: 70
        name: "knight_button"
    }

    @GUI::Button {
        fixed_width: 70
        fixed_height: 70
        name: "rook_button"
    }

    @GUI::Button {
        fixed_width: 70
        fixed_height: 70
        name: "bishop_button"
    }
}
