#pragma once

#include <LibGUI/GWidget.h>

class GSlider : public GWidget {
public:
    explicit GSlider(GWidget*);
    virtual ~GSlider() override;

    int value() const { return m_value; }
    int min() const { return m_min; }
    int max() const { return m_max; }

    void set_range(int min, int max);
    void set_value(int);

    void set_min(int min) { set_range(min, max()); }
    void set_max(int max) { set_range(min(), max); }

    int track_height() const { return 2; }
    int knob_width() const { return 8; }
    int knob_height() const { return 20; }

    Rect knob_rect() const;
    Rect inner_rect() const { return rect().shrunken(20, 0); }

    Function<void(int)> on_value_changed;

protected:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void leave_event(CEvent&) override;
    virtual void change_event(GEvent&) override;

private:
    void set_knob_hovered(bool);

    int m_value { 0 };
    int m_min { 0 };
    int m_max { 100 };

    bool m_knob_hovered { false };
    bool m_dragging { false };
    int m_drag_origin_value { 0 };
    Point m_drag_origin;
};
