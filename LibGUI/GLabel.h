#pragma once

#include "GWidget.h"
#include <AK/AKString.h>

class GLabel final : public GWidget {
public:
    explicit GLabel(GWidget* parent);
    virtual ~GLabel() override;

    String text() const { return m_text; }
    void set_text(String&&);

private:
    virtual void paint_event(GPaintEvent&) override;

    virtual const char* class_name() const override { return "GLabel"; }

    String m_text;
};

