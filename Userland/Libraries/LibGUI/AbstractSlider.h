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
    virtual ~AbstractSlider() override;

    void set_orientation(Orientation value);
    Orientation orientation() const { return m_orientation; }

    int value() const { return m_value; }
    int min() const { return m_min; }
    int max() const { return m_max; }
    int step() const { return m_step; }
    int page_step() const { return m_page_step; }
    bool jump_to_cursor() const { return m_jump_to_cursor; }

    void set_range(int min, int max);
    void set_value(int);

    void set_min(int min) { set_range(min, max()); }
    void set_max(int max) { set_range(min(), max); }
    void set_step(int step) { m_step = step; }
    void set_page_step(int page_step);
    void set_jump_to_cursor(bool b) { m_jump_to_cursor = b; }

    Function<void(int)> on_change;

protected:
    explicit AbstractSlider(Orientation = Orientation::Vertical);

private:
    void set_knob_hovered(bool);

    int m_value { 0 };
    int m_min { 0 };
    int m_max { 0 };
    int m_step { 1 };
    int m_page_step { 10 };
    bool m_jump_to_cursor { false };
    Orientation m_orientation { Orientation::Horizontal };
};

}
