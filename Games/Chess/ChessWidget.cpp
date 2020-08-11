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
#include <AK/String.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>

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

    for (unsigned rank = 0; rank < 8; ++rank) {
        for (unsigned file = 0; file < 8; ++file) {
            Gfx::IntRect tile_rect;
            if (side() == Chess::Colour::White) {
                tile_rect = { file * tile_width, (7 - rank) * tile_height, tile_width, tile_height };
            } else {
                tile_rect = { (7 - file) * tile_width, rank * tile_height, tile_width, tile_height };
            }

            painter.fill_rect(tile_rect, ((rank % 2) == (file % 2)) ? m_dark_square_color : m_light_square_color);

            Chess::Square square = { rank, file };
            if (!(m_dragging_piece && square == m_moving_square)) {
                auto bmp = m_pieces.get(board().get_piece(square));
                if (bmp.has_value()) {
                    painter.draw_scaled_bitmap(tile_rect, *bmp.value(), bmp.value()->rect());
                }
            }
        }
    }

    if (m_dragging_piece) {
        auto bmp = m_pieces.get(board().get_piece(m_moving_square));
        if (bmp.has_value()) {
            auto center = m_drag_point - Gfx::IntPoint(tile_width / 2, tile_height / 2);
            painter.draw_scaled_bitmap({ center, { tile_width, tile_height } }, *bmp.value(), bmp.value()->rect());
        }
    }
}

void ChessWidget::resize_event(GUI::ResizeEvent& event)
{
    GUI::Widget::resize_event(event);
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

    if (board().apply_move({ m_moving_square, target_square }) && board().game_result() != Chess::Result::NotFinished) {
        set_drag_enabled(false);
        update();

        String msg;
        switch (board().game_result()) {
        case Chess::Result::CheckMate:
            if (board().turn() == Chess::Colour::White) {
                msg = "Black wins by Checkmate.";
            } else {
                msg = "White wins by Checkmate.";
            }
            break;
        case Chess::Result::StaleMate:
            msg = "Draw by Stalemate.";
            break;
        case Chess::Result::FiftyMoveRule:
            msg = "Draw by 50 move rule.";
            break;
        case Chess::Result::ThreeFoldRepitition:
            msg = "Draw by threefold repitition.";
            break;
        default:
            ASSERT_NOT_REACHED();
        }
        GUI::MessageBox::show(window(), msg, "Game Over");
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
