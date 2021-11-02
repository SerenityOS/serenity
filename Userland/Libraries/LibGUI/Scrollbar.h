/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/AbstractSlider.h>

namespace GUI {

class Scrollbar : public AbstractSlider {
    C_OBJECT(Scrollbar);

public:
    virtual ~Scrollbar() override;

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
    explicit Scrollbar(Gfx::Orientation = Gfx::Orientation::Vertical);

    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mousewheel_event(MouseEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void change_event(Event&) override;

private:
    enum class GutterClickState {
        NotPressed,
        BeforeScrubber,
        AfterScrubber,
    } gutter_click_state
        = GutterClickState::NotPressed;

    int default_button_size() const { return 16; }
    int button_size() const { return length(orientation()) <= (default_button_size() * 2) ? length(orientation()) / 2 : default_button_size(); }
    int button_width() const { return orientation() == Orientation::Vertical ? width() : button_size(); }
    int button_height() const { return orientation() == Orientation::Horizontal ? height() : button_size(); }
    Gfx::IntRect decrement_button_rect() const;
    Gfx::IntRect increment_button_rect() const;
    Gfx::IntRect scrubber_rect() const;
    float unclamped_scrubber_size() const;
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
