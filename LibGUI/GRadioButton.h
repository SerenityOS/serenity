#pragma once

#include <LibGUI/GWidget.h>

class GRadioButton : public GWidget {
public:
    GRadioButton(const String& label, GWidget* parent);
    virtual ~GRadioButton() override;

    void set_label(const String&);
    String label() const { return m_label; }

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

protected:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;

private:
    virtual bool is_radio_button() const final { return true; }

    template<typename Callback> void for_each_in_group(Callback);
    static Size circle_size();

    String m_label;
    bool m_checked { false };
    bool m_changing { false };
    bool m_tracking { false };
};
