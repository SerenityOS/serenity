#pragma once

#include "GWidget.h"
#include <AK/AKString.h>

class GLabel final : public GWidget {
public:
    explicit GLabel(GWidget* parent);
    virtual ~GLabel() override;

    String text() const { return m_text; }
    void setText(String&&);

private:
    virtual void paintEvent(GPaintEvent&) override;
    virtual void mouseMoveEvent(GMouseEvent&) override;

    virtual const char* class_name() const override { return "GLabel"; }

    String m_text;
};

