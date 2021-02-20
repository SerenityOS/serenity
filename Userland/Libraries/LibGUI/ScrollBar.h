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
#include <LibGUI/AbstractSlider.h>

namespace GUI {

class ScrollBar : public AbstractSlider {
    C_OBJECT(ScrollBar);

public:
    virtual ~ScrollBar() override;

    bool is_scrollable() const { return max() != min(); }

    bool has_scrubber() const;

    enum Component {
        None,
        DecrementButton,
        IncrementButton,
        Gutter,
        Scrubber,
    };

protected:
    explicit ScrollBar(Gfx::Orientation = Gfx::Orientation::Vertical);

    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mousewheel_event(MouseEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void change_event(Event&) override;

private:
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

    int m_scrub_start_value { 0 };
    Gfx::IntPoint m_scrub_origin;

    Component m_hovered_component { Component::None };
    Component m_pressed_component { Component::None };
    Gfx::IntPoint m_last_mouse_position;

    RefPtr<Core::Timer> m_automatic_scrolling_timer;
};

}
