/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PromotionDialog.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Frame.h>

PromotionDialog::PromotionDialog(ChessWidget& chess_widget)
    : Dialog(chess_widget.window())
    , m_selected_piece(Chess::Type::None)
{
    set_title("Choose piece to promote to");
    set_icon(chess_widget.window()->icon());
    resize(70 * 4, 70);

    auto main_widget = set_main_widget<GUI::Frame>();
    main_widget->set_frame_style(Gfx::FrameStyle::SunkenContainer);
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<GUI::HorizontalBoxLayout>();

    for (auto const& type : { Chess::Type::Queen, Chess::Type::Knight, Chess::Type::Rook, Chess::Type::Bishop }) {
        auto& button = main_widget->add<GUI::Button>();
        button.set_fixed_height(70);
        button.set_icon(chess_widget.get_piece_graphic({ chess_widget.board().turn(), type }));
        button.on_click = [this, type](auto) {
            m_selected_piece = type;
            done(ExecResult::OK);
        };
    }
}

void PromotionDialog::event(Core::Event& event)
{
    Dialog::event(event);
}
