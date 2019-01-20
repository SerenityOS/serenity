#pragma once

#include "GWidget.h"
#include <AK/AKString.h>

class GCheckBox final : public GWidget {
public:
    explicit GCheckBox(GWidget* parent);
    virtual ~GCheckBox() override;

    String caption() const { return m_caption; }
    void set_caption(String&&);

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;

    virtual const char* class_name() const override { return "GCheckBox"; }

    String m_caption;
    bool m_checked { false };
};

