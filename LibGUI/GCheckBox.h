#pragma once

#include "GWidget.h"
#include <AK/AKString.h>
#include <AK/Function.h>

class GCheckBox final : public GWidget {
public:
    explicit GCheckBox(GWidget* parent);
    virtual ~GCheckBox() override;

    String caption() const { return m_caption; }
    void set_caption(String&&);

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

    Function<void(GCheckBox&, bool)> on_change;

    virtual const char* class_name() const override { return "GCheckBox"; }

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual bool accepts_focus() const override { return true; }

    String m_caption;
    bool m_checked { false };
    bool m_being_modified { false };
    bool m_tracking_cursor { false };
};

