/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <LibGUI/Frame.h>

class Bricks;

class BrickGame : public GUI::Frame {
    C_OBJECT(BrickGame);

public:
    // How should a particular space on the board be presented to the user
    enum class BoardSpace {
        FullyOn,
        ShadowHint,
        Off
    };

    virtual ~BrickGame() override = default;

    void reset();
    void toggle_pause();
    void set_show_shadow_hint(bool);
    bool show_shadow_hint();

private:
    BrickGame(StringView app_name);
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    void paint_sidebar_text(GUI::Painter&, int row, StringView);
    void paint_paused_text(GUI::Painter&);
    void paint_cell(GUI::Painter&, Gfx::IntRect, BoardSpace);
    ErrorOr<void> paint_game(GUI::Painter&, Gfx::IntRect const&);
    void game_over();

    enum class GameState {
        Idle = 0,
        Active
    };

    StringView const m_app_name;
    GameState m_state {};
    NonnullOwnPtr<Bricks> m_brick_game;
    unsigned m_high_score {};
    bool m_show_shadow_hint { true };

    Color m_back_color { Color::from_rgb(0x8fbc8f) };
    Color m_front_color { Color::Black };
    Color m_shadow_color { Color::from_rgb(0x729672) };
    Color m_hint_block_color { Color::from_rgb(0x485e48) };
};
