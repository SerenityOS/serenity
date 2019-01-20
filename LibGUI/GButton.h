#pragma once

#include "GWidget.h"
#include <AK/AKString.h>
#include <AK/Function.h>

class GButton final : public GWidget {
public:
    explicit GButton(GWidget* parent);
    virtual ~GButton() override;

    String caption() const { return m_caption; }
    void set_caption(String&&);

    Function<void(GButton&)> on_click;

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;

    virtual const char* class_name() const override { return "GButton"; }

    String m_caption;
    bool m_being_pressed { false };
};

