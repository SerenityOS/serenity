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

#include <LibCore/ConfigFile.h>
#include <LibGUI/Application.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/StandardCursor.h>

namespace Pong {

class Game final : public GUI::Widget {
    C_OBJECT(Game);

public:
    static const int game_width = 560;
    static const int game_height = 480;

    virtual ~Game() override;

private:
    Game();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    void reset();
    void reset_ball(int serve_to_player);
    void reset_paddles();
    void tick();
    void round_over(int player);
    void game_over(int player);
    void calculate_move();

    struct Ball {
        Gfx::FloatPoint position;
        Gfx::FloatPoint velocity;
        float radius { 4 };

        float x() const { return position.x(); }
        float y() const { return position.y(); }

        Gfx::FloatRect rect() const
        {
            return { x() - radius, y() - radius, radius * 2, radius * 2 };
        }
    };

    struct Paddle {
        Gfx::FloatRect rect;
        float speed { 5 };
        float width { 8 };
        float height { 28 };
        bool moving_up { false };
        bool moving_down { false };
        Gfx::Color color { Color::White };
    };

    struct Net {
        Gfx::Color color { Color::White };
        Gfx::FloatRect rect() const
        {
            return { (game_width / 2) - 1, 0, 2, game_height };
        }
    };

    Gfx::IntRect player_1_score_rect() const
    {
        int score_width = font().width(String::formatted("{}", m_player_1_score));
        return { (game_width / 2) + score_width + 2, 2, score_width, font().glyph_height() };
    }

    Gfx::IntRect player_2_score_rect() const
    {
        int score_width = font().width(String::formatted("{}", m_player_2_score));
        return { (game_width / 2) - score_width - 2, 2, score_width, font().glyph_height() };
    }

    Net m_net;
    Ball m_ball;
    Paddle m_player1_paddle;
    Paddle m_player2_paddle;

    int m_score_to_win = 21;
    int m_player_1_score = 0;
    int m_player_2_score = 0;
};

}
