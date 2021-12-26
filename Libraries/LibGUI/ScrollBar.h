/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Function.h>
#include <LibGUI/Widget.h>

namespace GUI {

class ScrollBar final : public Widget {
    C_OBJECT(ScrollBar)
public:
    virtual ~ScrollBar() override;

    Gfx::Orientation orientation() const { return m_orientation; }

    bool is_scrollable() const { return max() != min(); }

    int value() const { return m_value; }
    int min() const { return m_min; }
    int max() const { return m_max; }
    int page() const { return m_page; }
    int step() const { return m_step; }
    int big_step() const { return m_big_step; }

    void set_min(int min) { set_range(min, max(), page()); }
    void set_max(int max) { set_range(min(), max, page()); }
    void set_page(int page) { set_range(min(), max(), page); }
    void set_range(int min, int max) { set_range(min, max, page()); }
    void set_range(int min, int max, int page);
    void set_value(int value);
    void set_step(int step) { m_step = step; }
    void set_big_step(int big_step) { m_big_step = big_step; }
    bool has_scrubber() const;

    Function<void(int)> on_change;

    enum Component {
        None,
        DecrementButton,
        IncrementButton,
        Gutter,
        Scrubber,
    };

private:
    explicit ScrollBar(Gfx::Orientation = Gfx::Orientation::Vertical);

    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mousewheel_event(MouseEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void change_event(Event&) override;

    int default_button_size() const { return 16; }
    int button_size() const { return length(orientation()) <= (default_button_size() * 2) ? length(orientation()) / 2 : default_button_size(); }
    int button_width() const { return orientation() == Orientation::Vertical ? width() : button_size(); }
    int button_height() const { return orientation() == Orientation::Horizontal ? height() : button_size(); }
    Gfx::IntRect decrement_button_rect() const;
    Gfx::IntRect increment_button_rect() const;
    Gfx::IntRect decrement_gutter_rect() const;
    Gfx::IntRect increment_gutter_rect() const;
    Gfx::IntRect scrubber_rect() const;
    int unclamped_scrubber_size() const;
    int visible_scrubber_size() const;
    int scrubbable_range_in_pixels() const;
    void on_automatic_scrolling_timer_fired();
    void set_automatic_scrolling_active(bool, Component);

    void scroll_to_position(const Gfx::IntPoint&);
    void scroll_by_page(const Gfx::IntPoint&);

    Component component_at_position(const Gfx::IntPoint&);

    int m_min { 0 };
    int m_max { 0 };
    int m_page { 0 };
    int m_value { 0 };
    int m_step { 1 };
    int m_big_step { 5 };

    int m_scrub_start_value { 0 };
    Gfx::IntPoint m_scrub_origin;

    Gfx::Orientation m_orientation { Gfx::Orientation::Vertical };
    Component m_hovered_component { Component::None };
    Component m_pressed_component { Component::None };
    Gfx::IntPoint m_last_mouse_position;

    RefPtr<Core::Timer> m_automatic_scrolling_timer;
};

}
