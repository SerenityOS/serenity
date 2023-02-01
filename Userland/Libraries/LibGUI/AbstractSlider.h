/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace GUI {

class AbstractSlider : public Widget {
    C_OBJECT_ABSTRACT(AbstractSlider);

public:
    virtual ~AbstractSlider() override = default;

    void set_orientation(Orientation value);
    Orientation orientation() const { return m_orientation; }

    int value() const { return m_value; }
    int min() const { return m_min; }
    int max() const { return m_max; }
    int step() const { return m_step; }
    int page_step() const { return m_page_step; }
    bool jump_to_cursor() const { return m_jump_to_cursor; }

    bool is_min() const { return m_value == m_min; }
    bool is_max() const { return m_value == m_max; }

    void set_range(int min, int max);

    enum class DoClamp {
        Yes = 1,
        No = 0
    };
    virtual void set_value(int, AllowCallback = AllowCallback::Yes, DoClamp = DoClamp::Yes);

    void set_min(int min) { set_range(min, max()); }
    void set_max(int max) { set_range(min(), max); }
    void set_step(int step) { m_step = step; }
    void set_page_step(int page_step);
    void set_jump_to_cursor(bool b) { m_jump_to_cursor = b; }

    virtual void increase_slider_by(int delta) { set_value(value() + delta); }
    virtual void decrease_slider_by(int delta) { set_value(value() - delta); }
    virtual void increase_slider_by_page_steps(int page_steps) { set_value(value() + page_step() * page_steps); }
    virtual void decrease_slider_by_page_steps(int page_steps) { set_value(value() - page_step() * page_steps); }
    virtual void increase_slider_by_steps(int steps) { set_value(value() + step() * steps); }
    virtual void decrease_slider_by_steps(int steps) { set_value(value() - step() * steps); }

    Function<void(int)> on_change;

protected:
    explicit AbstractSlider(Orientation = Orientation::Vertical);

private:
    int m_value { 0 };
    int m_min { 0 };
    int m_max { 0 };
    int m_step { 1 };
    int m_page_step { 10 };
    bool m_jump_to_cursor { false };
    Orientation m_orientation { Orientation::Horizontal };
};

}
