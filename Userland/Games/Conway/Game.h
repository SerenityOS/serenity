/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

class Game : public GUI::Widget {
    C_OBJECT(Game)
public:
    virtual ~Game() override;
    void reset();

    int rows() const { return m_rows; };
    int columns() const { return m_columns; };

private:
    Game();
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;

    Gfx::IntRect first_cell_rect() const;
    void seed_universe();
    void update_universe();
    void interact_at(const Gfx::IntPoint&);

    const Gfx::Color m_alive_color { Color::Green };
    const Gfx::Color m_dead_color { Color::Black };
    const int m_rows { 200 };
    const int m_columns { 200 };
    const int m_sleep { 100 };
    GUI::MouseButton m_last_button { GUI::MouseButton::None };

    bool m_universe[200][200];
};
