/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 * Copyright (c) 2024, Daniel Gaston <tfd@tuta.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Engine.h"
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <LibChess/Chess.h>
#include <LibConfig/Listener.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/Bitmap.h>

namespace Chess {

class PGNParseError {
public:
    PGNParseError() = default;
    PGNParseError(String&& message)
        : m_message(move(message))
    {
    }

    String const& message() const { return m_message; }

    static PGNParseError from_string_formatted(ErrorOr<String> maybe_formatted_string)
    {
        if (maybe_formatted_string.is_error()) {
            return PGNParseError {};
        }
        return PGNParseError { maybe_formatted_string.release_value() };
    }

private:
    String m_message;
};

class ChessWidget final
    : public GUI::Frame
    , public Config::Listener {
    C_OBJECT_ABSTRACT(ChessWidget);

public:
    static ErrorOr<NonnullRefPtr<ChessWidget>> try_create();

    virtual ~ChessWidget() override = default;

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

    Chess::Board& board() { return m_board; }
    Chess::Board const& board() const { return m_board; }

    Chess::Board& board_playback() { return m_board_playback; }
    Chess::Board const& board_playback() const { return m_board_playback; }

    Chess::Color side() const { return m_side; }
    void set_side(Chess::Color side) { m_side = side; }

    void set_piece_set(StringView set);

    Optional<Chess::Square> mouse_to_square(GUI::MouseEvent& event) const;

    bool drag_enabled() const { return m_drag_enabled; }
    void set_drag_enabled(bool e) { m_drag_enabled = e; }
    RefPtr<Gfx::Bitmap const> get_piece_graphic(Chess::Piece const& piece) const;

    bool show_available_moves() const { return m_show_available_moves; }
    void set_show_available_moves(bool e) { m_show_available_moves = e; }

    ErrorOr<String> get_fen() const;
    ErrorOr<void, PGNParseError> import_pgn(Core::File&);
    ErrorOr<void> export_pgn(Core::File&) const;
    void update_move_display_widget(Chess::Board&);

    int resign();
    void flip_board();
    void reset();

    struct BoardTheme {
        StringView name;
        Gfx::Color dark_square_color;
        Gfx::Color light_square_color;
    };

    BoardTheme const& board_theme() const { return m_board_theme; }
    void set_board_theme(BoardTheme const& theme) { m_board_theme = theme; }
    void set_board_theme(StringView name);

    enum class PlaybackDirection {
        First,
        Backward,
        Forward,
        Last
    };

    void playback_move(PlaybackDirection);

    void set_engine(RefPtr<Engine> engine) { m_engine = engine; }
    void set_move_display_widget(RefPtr<GUI::TextEditor> move_display_widget) { m_move_display_widget = move_display_widget; }
    void set_white_time_label(RefPtr<GUI::Label> time_label) { m_white_time_label = time_label; }
    void set_black_time_label(RefPtr<GUI::Label> time_label) { m_black_time_label = time_label; }

    void input_engine_move();
    bool want_engine_move();

    void set_coordinates(bool coordinates) { m_coordinates = coordinates; }
    bool coordinates() const { return m_coordinates; }

    void set_highlight_checks(bool highlight_checks) { m_highlight_checks = highlight_checks; }
    bool highlight_checks() const { return m_highlight_checks; }

    void set_unlimited_time_control(bool unlimited) { m_unlimited_time_control = unlimited; }
    bool unlimited_time_control() const { return m_unlimited_time_control; }

    void set_time_control_seconds(i32 seconds) { m_time_control_seconds = seconds; }
    i32 time_control_seconds() const { return m_time_control_seconds; }

    void set_time_control_increment(i32 seconds) { m_time_control_increment = seconds; }
    i32 time_control_increment() const { return m_time_control_increment; }

    void initialize_timer();
    void update_time_labels(u32 white_time, u32 black_time);

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
        bool operator==(BoardMarking const& other) const { return from == other.from && to == other.to; }
    };

private:
    enum class ClaimDrawBehavior {
        Always,
        Prompt
    };

    ChessWidget() = default;

    virtual void config_string_did_change(StringView domain, StringView group, StringView key, StringView value) override;
    virtual void config_bool_did_change(StringView domain, StringView group, StringView key, bool value) override;

    bool check_game_over(ClaimDrawBehavior);
    void check_resign_on_time(StringView msg);
    void apply_increment(Chess::Move move);

    Chess::Board m_board;
    Chess::Board m_board_playback;
    bool m_playback { false };
    size_t m_playback_move_number { 0 };
    BoardMarking m_current_marking;
    Vector<BoardMarking> m_board_markings;
    BoardTheme m_board_theme { "Beige"sv, Gfx::Color::from_rgb(0xb58863), Gfx::Color::from_rgb(0xf0d9b5) };
    Gfx::Color m_move_highlight_color { Gfx::Color::from_argb(0x66ccee00) };
    Gfx::Color m_marking_primary_color { Gfx::Color::from_argb(0x66ff0000) };
    Gfx::Color m_marking_alternate_color { Gfx::Color::from_argb(0x66ffaa00) };
    Gfx::Color m_marking_secondary_color { Gfx::Color::from_argb(0x6655dd55) };
    Chess::Color m_side { Chess::Color::White };
    HashMap<Chess::Piece, RefPtr<Gfx::Bitmap const>> m_pieces;
    bool m_any_piece_images_are_missing { false };
    Chess::Square m_moving_square { 50, 50 };
    Gfx::IntPoint m_drag_point;
    bool m_dragging_piece { false };
    bool m_drag_enabled { true };
    bool m_show_available_moves { true };
    Vector<Chess::Square> m_available_moves;
    RefPtr<Engine> m_engine;
    bool m_coordinates { true };
    bool m_highlight_checks { true };
    RefPtr<GUI::TextEditor> m_move_display_widget;
    RefPtr<GUI::Label> m_white_time_label;
    RefPtr<GUI::Label> m_black_time_label;
    bool m_unlimited_time_control { true };
    i32 m_time_control_seconds { 0 };
    i32 m_time_control_increment { 0 };
    i32 m_white_time_elapsed { 0 };
    i32 m_black_time_elapsed { 0 };
    RefPtr<Core::Timer> m_timer;
};

}
