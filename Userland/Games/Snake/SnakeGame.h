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

class SnakeGame : public GUI::Frame {
    C_OBJECT(SnakeGame);

public:
    virtual ~SnakeGame() override = default;

    void reset();

private:
    SnakeGame();
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    struct Coordinate {
        int row { 0 };
        int column { 0 };

        bool operator==(const Coordinate& other) const
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
    bool is_available(const Coordinate&);
    void queue_velocity(int v, int h);
    const Velocity& last_velocity() const;
    Gfx::IntRect cell_rect(const Coordinate&) const;
    Gfx::IntRect score_rect() const;
    Gfx::IntRect high_score_rect() const;

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
    String m_score_text;
    unsigned m_high_score { 0 };
    String m_high_score_text;

    NonnullRefPtrVector<Gfx::Bitmap> m_fruit_bitmaps;
};
