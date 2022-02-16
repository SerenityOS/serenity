/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>

namespace Breakout {

class Game final : public GUI::Widget {
    C_OBJECT(Game);

public:
    static const int game_width = 480;
    static const int game_height = 500;

    virtual ~Game() override = default;

    void set_paused(bool paused);

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

    Gfx::IntRect lives_left_rect() const
    {
        int msg_width = font().width(String::formatted("Lives: {}", m_lives));
        return { (game_width - msg_width - 2), 2, msg_width, font().glyph_height() };
    }

    Gfx::IntRect pause_rect() const
    {
        const char* msg = m_cheater ? "C H E A T E R" : "P A U S E D";
        int msg_width = font().width(msg);
        int msg_height = font().glyph_height();
        return { (game_width / 2) - (msg_width / 2), (game_height / 2) - (msg_height / 2), msg_width, msg_height };
    }

    bool m_paused;
    int m_lives;
    int m_board;
    long m_pause_count;
    bool m_cheater;
    Ball m_ball;
    Paddle m_paddle;
    Vector<Brick> m_bricks;
};

}
