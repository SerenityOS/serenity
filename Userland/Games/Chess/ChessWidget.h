/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Engine.h"
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <LibChess/Chess.h>
#include <LibGUI/Frame.h>
#include <LibGfx/Bitmap.h>

class ChessWidget final : public GUI::Frame {
    C_OBJECT(ChessWidget);

public:
    virtual ~ChessWidget() override;

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

    Chess::Board& board() { return m_board; };
    const Chess::Board& board() const { return m_board; };

    Chess::Board& board_playback() { return m_board_playback; };
    const Chess::Board& board_playback() const { return m_board_playback; };

    Chess::Color side() const { return m_side; };
    void set_side(Chess::Color side) { m_side = side; };

    void set_piece_set(StringView set);
    const String& piece_set() const { return m_piece_set; };

    Chess::Square mouse_to_square(GUI::MouseEvent& event) const;

    bool drag_enabled() const { return m_drag_enabled; }
    void set_drag_enabled(bool e) { m_drag_enabled = e; }
    RefPtr<Gfx::Bitmap> get_piece_graphic(const Chess::Piece& piece) const;

    bool show_available_moves() const { return m_show_available_moves; }
    void set_show_available_moves(bool e) { m_show_available_moves = e; }

    String get_fen() const;
    bool import_pgn(StringView import_path);
    bool export_pgn(StringView export_path) const;

    int resign();
    void flip_board();
    void reset();

    struct BoardTheme {
        String name;
        Color dark_square_color;
        Color light_square_color;
    };

    const BoardTheme& board_theme() const { return m_board_theme; }
    void set_board_theme(const BoardTheme& theme) { m_board_theme = theme; }
    void set_board_theme(StringView name);

    enum class PlaybackDirection {
        First,
        Backward,
        Forward,
        Last
    };

    void playback_move(PlaybackDirection);

    void set_engine(RefPtr<Engine> engine) { m_engine = engine; }

    void input_engine_move();
    bool want_engine_move();

    void set_coordinates(bool coordinates) { m_coordinates = coordinates; }
    bool coordinates() const { return m_coordinates; }

    struct BoardMarking {
        Chess::Square from { 50, 50 };
        Chess::Square to { 50, 50 };
        bool alternate_color { false };
        bool secondary_color { false };
        enum class Type {
            Square,
            Arrow,
            None
        };
        Type type() const
        {
            if (from.in_bounds() && to.in_bounds() && from != to)
                return Type::Arrow;
            else if ((from.in_bounds() && !to.in_bounds()) || (from.in_bounds() && to.in_bounds() && from == to))
                return Type::Square;

            return Type::None;
        }
        bool operator==(const BoardMarking& other) const { return from == other.from && to == other.to; }
    };

private:
    ChessWidget();

    Chess::Board m_board;
    Chess::Board m_board_playback;
    bool m_playback { false };
    size_t m_playback_move_number { 0 };
    BoardMarking m_current_marking;
    Vector<BoardMarking> m_board_markings;
    BoardTheme m_board_theme { "Beige", Color::from_rgb(0xb58863), Color::from_rgb(0xf0d9b5) };
    Color m_move_highlight_color { Color::from_rgba(0x66ccee00) };
    Color m_marking_primary_color { Color::from_rgba(0x66ff0000) };
    Color m_marking_alternate_color { Color::from_rgba(0x66ffaa00) };
    Color m_marking_secondary_color { Color::from_rgba(0x6655dd55) };
    Chess::Color m_side { Chess::Color::White };
    HashMap<Chess::Piece, RefPtr<Gfx::Bitmap>> m_pieces;
    String m_piece_set;
    Chess::Square m_moving_square { 50, 50 };
    Gfx::IntPoint m_drag_point;
    bool m_dragging_piece { false };
    bool m_drag_enabled { true };
    bool m_show_available_moves { true };
    Vector<Chess::Square> m_available_moves;
    RefPtr<Engine> m_engine;
    bool m_coordinates { true };
};
