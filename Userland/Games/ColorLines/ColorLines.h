/*
 * Copyright (c) 2022, Oleg Kosenkov <oleg@kosenkov.ca>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "MarblePath.h"
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibGUI/Frame.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Forward.h>

class MarbleBoard;

class ColorLines : public GUI::Frame {
    C_OBJECT(ColorLines);

public:
    virtual ~ColorLines() override = default;

    void reset();

private:
    enum class GameState {
        Idle = 0,          // No marble is selected, waiting for marble selection
        StartingGame,      // Game is starting
        GeneratingMarbles, // Three new marbles are being generated
        MarbleSelected,    // Marble is selected, waiting for the target cell selection
        MarbleMoving,      // Selected marble is moving to the target cell
        MarblesRemoving,   // Selected marble has completed the move and some marbles are being removed from the board
        CheckingMarbles,   // Checking whether marbles on the board form lines of 5 or more marbles
        GameOver           // Game is over
    };

    ColorLines(StringView app_name);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    void set_game_state(GameState state);
    void restart_timer(int milliseconds);

    using Point = Gfx::IntPoint;
    using BitmapArray = Vector<NonnullRefPtr<Gfx::Bitmap const>>;

    StringView const m_app_name;
    GameState m_game_state { GameState::Idle };
    NonnullOwnPtr<MarbleBoard> m_board;
    BitmapArray const m_marble_bitmaps;
    BitmapArray const m_trace_bitmaps;
    RefPtr<Gfx::BitmapFont> m_score_font;
    MarblePath m_marble_path {};
    int m_marble_animation_frame {};
    unsigned m_score {};
    unsigned m_high_score {};

    static BitmapArray build_marble_color_bitmaps();
    static BitmapArray build_marble_trace_bitmaps();

    static constexpr auto marble_pixel_size { 40 };
    static constexpr auto board_vertical_margin { 45 };
    static constexpr auto board_cell_dimension = Gfx::IntSize { 48, 48 };
    static constexpr auto number_of_marble_trace_bitmaps { 4 };
    static constexpr auto tile_color { Color::from_rgb(0xc0c0c0) };
    static constexpr auto text_color { Color::from_rgb(0x00a0ff) };

    enum AnimationFrames {
        marble_default = 0,
        marble_at_top = 2,
        marble_preview = 18,
        marble_generating_start = 21,
        marble_generating_end = 17,
        marble_removing_start = 7,
        marble_removing_end = 16,
        number_of_marble_bounce_frames = 7
    };

    enum TimerIntervals {
        generating_marbles = 80,
        removing_marbles = 60,
        selected_marble = 70,
        moving_marble = 28
    };
};
