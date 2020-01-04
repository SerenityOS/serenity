#pragma once

#include <LibGUI/GWidget.h>

class GSlider : public GWidget {
    C_OBJECT(GSlider)
public:
    enum class KnobSizeMode {
        Fixed,
        Proportional,
    };

    virtual ~GSlider() override;

    Orientation orientation() const { return m_orientation; }

    int value() const { return m_value; }
    int min() const { return m_min; }
    int max() const { return m_max; }

    void set_range(int min, int max);
    void set_value(int);

    void set_min(int min) { set_range(min, max()); }
    void set_max(int max) { set_range(min(), max); }

    void set_knob_size_mode(KnobSizeMode mode) { m_knob_size_mode = mode; }
    KnobSizeMode knob_size_mode() const { return m_knob_size_mode; }

    int track_size() const { return 2; }
    int knob_fixed_primary_size() const { return 8; }
    int knob_secondary_size() const { return 20; }

    bool knob_dragging() const { return m_dragging; }
    Rect knob_rect() const;

    Rect inner_rect() const
    {
        if (orientation() == Orientation::Horizontal)
            return rect().shrunken(20, 0);
        return rect().shrunken(0, 20);
    }

    Function<void(int)> on_value_changed;

protected:
    explicit GSlider(GWidget*);
    explicit GSlider(Orientation, GWidget*);

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
    KnobSizeMode m_knob_size_mode { KnobSizeMode::Fixed };
    Orientation m_orientation { Orientation::Horizontal };
};
