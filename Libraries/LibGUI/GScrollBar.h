#pragma once

#include <AK/Function.h>
#include <LibCore/CTimer.h>
#include <LibGUI/GWidget.h>

class GScrollBar final : public GWidget {
    C_OBJECT(GScrollBar)
public:
    virtual ~GScrollBar() override;

    Orientation orientation() const { return m_orientation; }

    bool is_scrollable() const { return max() != min(); }

    int value() const { return m_value; }
    int min() const { return m_min; }
    int max() const { return m_max; }
    int step() const { return m_step; }
    int big_step() const { return m_big_step; }

    void set_min(int min) { set_range(min, max()); }
    void set_max(int max) { set_range(min(), max); }
    void set_range(int min, int max);
    void set_value(int value);
    void set_step(int step) { m_step = step; }
    void set_big_step(int big_step) { m_big_step = big_step; }
    bool has_scrubber() const;

    Function<void(int)> on_change;

    enum Component {
        Invalid,
        DecrementButton,
        IncrementButton,
        Gutter,
        Scrubber,
    };

private:
    explicit GScrollBar(GWidget* parent);
    explicit GScrollBar(Orientation, GWidget* parent);

    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mousewheel_event(GMouseEvent&) override;
    virtual void leave_event(CEvent&) override;
    virtual void change_event(GEvent&) override;

    int default_button_size() const { return 16; }
    int button_size() const { return length(orientation()) <= (default_button_size() * 2) ? length(orientation()) / 2 : default_button_size(); }
    int button_width() const { return orientation() == Orientation::Vertical ? width() : button_size(); }
    int button_height() const { return orientation() == Orientation::Horizontal ? height() : button_size(); }
    Rect decrement_button_rect() const;
    Rect increment_button_rect() const;
    Rect decrement_gutter_rect() const;
    Rect increment_gutter_rect() const;
    Rect scrubber_rect() const;
    int scrubber_size() const;
    int scrubbable_range_in_pixels() const;
    void on_automatic_scrolling_timer_fired();
    void set_automatic_scrolling_active(bool);

    int m_min { 0 };
    int m_max { 0 };
    int m_value { 0 };
    int m_step { 1 };
    int m_big_step { 5 };

    bool m_scrubbing { false };
    int m_scrub_start_value { 0 };
    Point m_scrub_origin;

    Orientation m_orientation { Orientation::Vertical };
    Component m_hovered_component { Component::Invalid };
    bool m_scrubber_in_use { false };

    enum class AutomaticScrollingDirection {
        None = 0,
        Decrement,
        Increment,
    };

    AutomaticScrollingDirection m_automatic_scrolling_direction { AutomaticScrollingDirection::None };
    RefPtr<CTimer> m_automatic_scrolling_timer;
};
