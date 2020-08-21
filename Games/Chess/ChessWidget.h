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

#pragma once

#include "Engine.h"
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <LibChess/Chess.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>

class ChessWidget final : public GUI::Widget {
    C_OBJECT(ChessWidget)
public:
    ChessWidget();
    ChessWidget(const StringView& set);
    virtual ~ChessWidget() override;

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;

    Chess::Board& board() { return m_board; };

    Chess::Colour side() const { return m_side; };
    void set_side(Chess::Colour side) { m_side = side; };

    void set_piece_set(const StringView& set);
    const String& piece_set() const { return m_piece_set; };

    Chess::Square mouse_to_square(GUI::MouseEvent& event) const;

    bool drag_enabled() const { return m_drag_enabled; }
    void set_drag_enabled(bool e) { m_drag_enabled = e; }
    RefPtr<Gfx::Bitmap> get_piece_graphic(const Chess::Piece& piece) const;

    void reset();

    struct BoardTheme {
        String name;
        Color dark_square_color;
        Color light_square_color;
    };

    const BoardTheme& board_theme() const { return m_board_theme; }
    void set_board_theme(const BoardTheme& theme) { m_board_theme = theme; }
    void set_board_theme(const StringView& name);

    void set_engine(RefPtr<Engine> engine) { m_engine = engine; }

    void maybe_input_engine_move();

    void set_coordinates(bool coordinates) { m_coordinates = coordinates; }
    bool coordinates() const { return m_coordinates; }

private:
    Chess::Board m_board;
    BoardTheme m_board_theme { "Beige", Color::from_rgb(0xb58863), Color::from_rgb(0xf0d9b5) };
    Color m_move_highlight_color { Color::from_rgba(0x66ccee00) };
    Chess::Colour m_side { Chess::Colour::White };
    HashMap<Chess::Piece, RefPtr<Gfx::Bitmap>> m_pieces;
    String m_piece_set;
    Chess::Square m_moving_square { 50, 50 };
    Gfx::IntPoint m_drag_point;
    bool m_dragging_piece { false };
    bool m_drag_enabled { true };
    RefPtr<Engine> m_engine;
    bool m_coordinates { true };
};
