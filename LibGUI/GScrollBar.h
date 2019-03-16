#pragma once

#include <LibGUI/GWidget.h>
#include <AK/Function.h>

class GScrollBar final : public GWidget {
public:
    explicit GScrollBar(Orientation, GWidget* parent);
    virtual ~GScrollBar() override;

    Orientation orientation() const { return m_orientation; }

    int value() const { return m_value; }
    int min() const { return m_min; }
    int max() const { return m_max; }
    int step() const { return m_step; }
    int big_step() const { return m_big_step; }

    void set_range(int min, int max);
    void set_value(int value);
    void set_step(int step) { m_step = step; }
    void set_big_step(int big_step) { m_big_step = big_step; }
    bool has_scrubber() const;

    Function<void(int)> on_change;

    virtual const char* class_name() const override { return "GScrollBar"; }

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;

    int button_size() const { return orientation() == Orientation::Vertical ? width() : height(); }
    Rect up_button_rect() const;
    Rect down_button_rect() const;
    Rect upper_gutter_rect() const;
    Rect lower_gutter_rect() const;
    Rect scrubber_rect() const;
    int scrubbable_range_in_pixels() const;

    int m_min { 0 };
    int m_max { 0 };
    int m_value { 0 };
    int m_step { 1 };
    int m_big_step { 5 };

    bool m_scrubbing { false };
    int m_scrub_start_value { 0 };
    Point m_scrub_origin;

    Orientation m_orientation { Orientation::Vertical };
};
