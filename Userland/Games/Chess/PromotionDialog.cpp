/*
 * Copyright (c) 2020-2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PromotionDialog.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Frame.h>

namespace Chess {

ErrorOr<NonnullRefPtr<PromotionDialog>> PromotionDialog::try_create(ChessWidget& chess_widget)
{
    auto promotion_widget = TRY(Chess::PromotionWidget::try_create());
    auto promotion_dialog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PromotionDialog(move(promotion_widget), chess_widget)));
    return promotion_dialog;
}

PromotionDialog::PromotionDialog(NonnullRefPtr<Chess::PromotionWidget> promotion_widget, ChessWidget& chess_widget)
    : Dialog(chess_widget.window())
    , m_selected_piece(Chess::Type::None)
{
    set_title("Choose piece to promote to");
    set_icon(chess_widget.window()->icon());
    set_main_widget(promotion_widget);

    auto initialize_promotion_button = [&](StringView button_name, Chess::Type piece) {
        auto button = promotion_widget->find_descendant_of_type_named<GUI::Button>(button_name);
        button->set_icon(chess_widget.get_piece_graphic({ chess_widget.board().turn(), piece }));
        button->on_click = [this, piece](auto) {
            m_selected_piece = piece;
            done(ExecResult::OK);
        };
    };

    initialize_promotion_button("queen_button"sv, Type::Queen);
    initialize_promotion_button("knight_button"sv, Type::Knight);
    initialize_promotion_button("rook_button"sv, Type::Rook);
    initialize_promotion_button("bishop_button"sv, Type::Bishop);
}

void PromotionDialog::event(Core::Event& event)
{
    Dialog::event(event);
}

}
