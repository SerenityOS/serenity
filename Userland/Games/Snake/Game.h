/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibGUI/Frame.h>

namespace Snake {

class Game : public GUI::Frame {
    C_OBJECT(Game);

public:
    virtual ~Game() override = default;

    void start();
    void pause();
    void reset();

    void set_snake_base_color(Color color);

    Function<bool(u32)> on_score_update;

private:
    Game();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    struct Coordinate {
        int row { 0 };
        int column { 0 };

        bool operator==(Coordinate const& other) const
        {
            return row == other.row && column == other.column;
        }
    };

    struct Velocity {
        int vertical { 0 };
        int horizontal { 0 };
    };

    void game_over();
    void spawn_fruit();
    bool is_available(Coordinate const&);
    void queue_velocity(int v, int h);
    Velocity const& last_velocity() const;
    Gfx::IntRect cell_rect(Coordinate const&) const;

    int m_rows { 20 };
    int m_columns { 20 };

    Velocity m_velocity { 0, 1 };
    Velocity m_last_velocity { 0, 1 };

    CircularQueue<Velocity, 10> m_velocity_queue;

    Coordinate m_head;
    Vector<Coordinate> m_tail;

    Coordinate m_fruit;
    int m_fruit_type { 0 };

    size_t m_length { 0 };
    unsigned m_score { 0 };
    bool m_is_new_high_score { false };

    NonnullRefPtrVector<Gfx::Bitmap> m_food_bitmaps;

    Gfx::Color m_snake_base_color { Color::Yellow };
};

}
