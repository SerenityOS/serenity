#pragma once

#include "GWidget.h"
#include <AK/AKString.h>
#include <AK/Function.h>

class GButton final : public GWidget {
public:
    explicit GButton(GWidget* parent);
    virtual ~GButton() override;

    String caption() const { return m_caption; }
    void setCaption(String&&);

    Function<void(GButton&)> onClick;

private:
    virtual void paintEvent(GPaintEvent&) override;
    virtual void mouseDownEvent(GMouseEvent&) override;
    virtual void mouseUpEvent(GMouseEvent&) override;

    virtual const char* class_name() const override { return "GButton"; }

    String m_caption;
    bool m_beingPressed { false };
};

