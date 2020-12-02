/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/Widget.h>

namespace Breakout {

class Game final : public GUI::Widget {
    C_OBJECT(Game);

public:
    static const int game_width = 480;
    static const int game_height = 500;

    virtual ~Game() override;

private:
    Game();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    void reset();
    void reset_ball();
    void reset_paddle();
    void generate_bricks();
    void tick();
    void hurt();
    void win();

    struct Ball {
        Gfx::FloatPoint position;
        Gfx::FloatPoint velocity;
        float radius { 8 };

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
        bool moving_left { false };
        bool moving_right { false };
    };

    struct Brick {
        Gfx::FloatRect rect;
        Gfx::Color color;
        bool dead { false };

        Brick(int row, int column, Gfx::Color c, int brick_width = 40, int brick_height = 12, int field_left_offset = 30, int field_top_offset = 30, int brick_spacing = 3)
        {
            rect = {
                field_left_offset + (column * brick_width) + (column * brick_spacing),
                field_top_offset + (row * brick_height) + (row * brick_spacing),
                brick_width,
                brick_height
            };
            color = c;
        }
    };

    int m_board;
    Ball m_ball;
    Paddle m_paddle;
    Vector<Brick> m_bricks;
};

}
