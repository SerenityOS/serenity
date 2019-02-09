#pragma once

#include <LibGUI/GWidget.h>
#include <AK/Function.h>

class GScrollBar final : public GWidget {
public:
    explicit GScrollBar(GWidget* parent);
    virtual ~GScrollBar() override;

    int value() const { return m_value; }
    int min() const { return m_min; }
    int max() const { return m_max; }
    int step() const { return m_step; }
    int big_step() const { return m_big_step; }

    void set_range(int min, int max);
    void set_value(int value);
    void set_step(int step) { m_step = step; }
    void set_big_step(int big_step) { m_big_step = big_step; }

    Function<void(int)> on_change;

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual const char* class_name() const override { return "GScrollBar"; }

    int button_size() const { return 16; }
    Rect up_button_rect() const;
    Rect down_button_rect() const;
    Rect pgup_rect() const;
    Rect pgdn_rect() const;
    Rect scrubber_rect() const;

    int m_min { 0 };
    int m_max { 0 };
    int m_value { 0 };
    int m_step { 1 };
    int m_big_step { 5 };
};

