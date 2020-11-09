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
        int x { 0 };
        int y { 0 };
        int radius { 8 };
        int vx { 0 };
        int vy { 0 };

        Gfx::IntRect rect() const
        {
            return { x - radius, y - radius, radius * 2, radius * 2 };
        }
    };

    struct Paddle {
        Gfx::IntRect rect;
        int speed { 5 };
        bool moving_left { false };
        bool moving_right { false };
    };

    struct Brick {
        Gfx::IntRect rect;
        Gfx::Color color;
        bool dead { false };
    };

    Ball m_ball;
    Paddle m_paddle;
    Vector<Brick> m_bricks;
};

}
