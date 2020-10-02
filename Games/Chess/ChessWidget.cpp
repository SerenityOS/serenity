/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "ChessWidget.h"
#include "PromotionDialog.h"
#include <AK/String.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>

ChessWidget::ChessWidget(const StringView& set)
{
    set_piece_set(set);
}

ChessWidget::ChessWidget()
    : ChessWidget("test")
{
}

ChessWidget::~ChessWidget()
{
}

void ChessWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Widget::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    size_t tile_width = width() / 8;
    size_t tile_height = height() / 8;
    unsigned coord_rank_file = (side() == Chess::Colour::White) ? 0 : 7;

    Chess::Square::for_each([&](Chess::Square sq) {
        Gfx::IntRect tile_rect;
        if (side() == Chess::Colour::White) {
            tile_rect = { sq.file * tile_width, (7 - sq.rank) * tile_height, tile_width, tile_height };
        } else {
            tile_rect = { (7 - sq.file) * tile_width, sq.rank * tile_height, tile_width, tile_height };
        }

        painter.fill_rect(tile_rect, (sq.is_light()) ? board_theme().light_square_color : board_theme().dark_square_color);

        if (board().last_move().has_value() && (board().last_move().value().to == sq || board().last_move().value().from == sq)) {
            painter.fill_rect(tile_rect, m_move_highlight_color);
        }

        if (m_coordinates) {
            auto coord = sq.to_algebraic();
            auto text_color = (sq.is_light()) ? board_theme().dark_square_color : board_theme().light_square_color;

            auto shrunken_rect = tile_rect;
            shrunken_rect.shrink(4, 4);
            if (sq.rank == coord_rank_file)
                painter.draw_text(shrunken_rect, coord.substring_view(0, 1), Gfx::Font::default_bold_font(), Gfx::TextAlignment::BottomRight, text_color);

            if (sq.file == coord_rank_file)
                painter.draw_text(shrunken_rect, coord.substring_view(1, 1), Gfx::Font::default_bold_font(), Gfx::TextAlignment::TopLeft, text_color);
        }

        if (!(m_dragging_piece && sq == m_moving_square)) {
            auto bmp = m_pieces.get(board().get_piece(sq));
            if (bmp.has_value()) {
                painter.draw_scaled_bitmap(tile_rect, *bmp.value(), bmp.value()->rect());
            }
        }

        return IterationDecision::Continue;
    });

    if (m_dragging_piece) {
        auto bmp = m_pieces.get(board().get_piece(m_moving_square));
        if (bmp.has_value()) {
            auto center = m_drag_point - Gfx::IntPoint(tile_width / 2, tile_height / 2);
            painter.draw_scaled_bitmap({ center, { tile_width, tile_height } }, *bmp.value(), bmp.value()->rect());
        }
    }
}

void ChessWidget::mousedown_event(GUI::MouseEvent& event)
{
    GUI::Widget::mousedown_event(event);
    auto square = mouse_to_square(event);
    auto piece = board().get_piece(square);
    if (drag_enabled() && piece.colour == board().turn()) {
        m_dragging_piece = true;
        m_drag_point = event.position();
        m_moving_square = square;
        update();
    }
}

void ChessWidget::mouseup_event(GUI::MouseEvent& event)
{
    GUI::Widget::mouseup_event(event);
    if (!m_dragging_piece)
        return;

    m_dragging_piece = false;

    auto target_square = mouse_to_square(event);

    Chess::Move move = { m_moving_square, target_square };
    if (board().is_promotion_move(move)) {
        auto promotion_dialog = PromotionDialog::construct(*this);
        if (promotion_dialog->exec() == PromotionDialog::ExecOK)
            move.promote_to = promotion_dialog->selected_piece();
    }

    if (board().apply_move(move)) {
        if (board().game_result() != Chess::Board::Result::NotFinished) {

            bool over = true;
            String msg;
            switch (board().game_result()) {
            case Chess::Board::Result::CheckMate:
                if (board().turn() == Chess::Colour::White) {
                    msg = "Black wins by Checkmate.";
                } else {
                    msg = "White wins by Checkmate.";
                }
                break;
            case Chess::Board::Result::StaleMate:
                msg = "Draw by Stalemate.";
                break;
            case Chess::Board::Result::FiftyMoveRule:
                update();
                if (GUI::MessageBox::show(window(), "50 moves have elapsed without a capture. Claim Draw?", "Claim Draw?",
                        GUI::MessageBox::Type::Information, GUI::MessageBox::InputType::YesNo)
                    == GUI::Dialog::ExecYes) {
                    msg = "Draw by 50 move rule.";
                } else {
                    over = false;
                }
                break;
            case Chess::Board::Result::SeventyFiveMoveRule:
                msg = "Draw by 75 move rule.";
                break;
            case Chess::Board::Result::ThreeFoldRepetition:
                update();
                if (GUI::MessageBox::show(window(), "The same board state has repeated three times. Claim Draw?", "Claim Draw?",
                        GUI::MessageBox::Type::Information, GUI::MessageBox::InputType::YesNo)
                    == GUI::Dialog::ExecYes) {
                    msg = "Draw by threefold repetition.";
                } else {
                    over = false;
                }
                break;
            case Chess::Board::Result::FiveFoldRepetition:
                msg = "Draw by fivefold repetition.";
                break;
            case Chess::Board::Result::InsufficientMaterial:
                msg = "Draw by insufficient material.";
                break;
            default:
                ASSERT_NOT_REACHED();
            }
            if (over) {
                set_drag_enabled(false);
                update();
                GUI::MessageBox::show(window(), msg, "Game Over", GUI::MessageBox::Type::Information);
            }
        } else {
            maybe_input_engine_move();
        }
    }

    update();
}

void ChessWidget::mousemove_event(GUI::MouseEvent& event)
{
    GUI::Widget::mousemove_event(event);
    if (!m_dragging_piece)
        return;

    m_drag_point = event.position();
    update();
}

static String set_path = String("/res/icons/chess/sets/");

static RefPtr<Gfx::Bitmap> get_piece(const StringView& set, const StringView& image)
{
    StringBuilder builder;
    builder.append(set_path);
    builder.append(set);
    builder.append('/');
    builder.append(image);
    return Gfx::Bitmap::load_from_file(builder.build());
}

void ChessWidget::set_piece_set(const StringView& set)
{
    m_piece_set = set;
    m_pieces.set({ Chess::Colour::White, Chess::Type::Pawn }, get_piece(set, "white-pawn.png"));
    m_pieces.set({ Chess::Colour::Black, Chess::Type::Pawn }, get_piece(set, "black-pawn.png"));
    m_pieces.set({ Chess::Colour::White, Chess::Type::Knight }, get_piece(set, "white-knight.png"));
    m_pieces.set({ Chess::Colour::Black, Chess::Type::Knight }, get_piece(set, "black-knight.png"));
    m_pieces.set({ Chess::Colour::White, Chess::Type::Bishop }, get_piece(set, "white-bishop.png"));
    m_pieces.set({ Chess::Colour::Black, Chess::Type::Bishop }, get_piece(set, "black-bishop.png"));
    m_pieces.set({ Chess::Colour::White, Chess::Type::Rook }, get_piece(set, "white-rook.png"));
    m_pieces.set({ Chess::Colour::Black, Chess::Type::Rook }, get_piece(set, "black-rook.png"));
    m_pieces.set({ Chess::Colour::White, Chess::Type::Queen }, get_piece(set, "white-queen.png"));
    m_pieces.set({ Chess::Colour::Black, Chess::Type::Queen }, get_piece(set, "black-queen.png"));
    m_pieces.set({ Chess::Colour::White, Chess::Type::King }, get_piece(set, "white-king.png"));
    m_pieces.set({ Chess::Colour::Black, Chess::Type::King }, get_piece(set, "black-king.png"));
}

Chess::Square ChessWidget::mouse_to_square(GUI::MouseEvent& event) const
{
    size_t tile_width = width() / 8;
    size_t tile_height = height() / 8;

    if (side() == Chess::Colour::White) {
        return { 7 - (event.y() / tile_height), event.x() / tile_width };
    } else {
        return { event.y() / tile_height, 7 - (event.x() / tile_width) };
    }
}

RefPtr<Gfx::Bitmap> ChessWidget::get_piece_graphic(const Chess::Piece& piece) const
{
    return m_pieces.get(piece).value();
}

void ChessWidget::reset()
{
    m_board = Chess::Board();
    m_side = (arc4random() % 2) ? Chess::Colour::White : Chess::Colour::Black;
    m_drag_enabled = true;
    maybe_input_engine_move();
    update();
}

void ChessWidget::set_board_theme(const StringView& name)
{
    // FIXME: Add some kind of themes.json
    // The following Colours have been taken from lichess.org, but i'm pretty sure they took them from chess.com.
    if (name == "Beige") {
        m_board_theme = { "Beige", Color::from_rgb(0xb58863), Color::from_rgb(0xf0d9b5) };
    } else if (name == "Green") {
        m_board_theme = { "Green", Color::from_rgb(0x86a666), Color::from_rgb(0xffffdd) };
    } else if (name == "Blue") {
        m_board_theme = { "Blue", Color::from_rgb(0x8ca2ad), Color::from_rgb(0xdee3e6) };
    } else {
        set_board_theme("Beige");
    }
}

void ChessWidget::maybe_input_engine_move()
{
    if (!m_engine || board().turn() == side())
        return;

    bool drag_was_enabled = drag_enabled();
    if (drag_was_enabled)
        set_drag_enabled(false);

    m_engine->get_best_move(board(), 4000, [this, drag_was_enabled](Chess::Move move) {
        set_drag_enabled(drag_was_enabled);
        ASSERT(board().apply_move(move));
        update();
    });
}
