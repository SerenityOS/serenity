#pragma once

#include "Widget.h"
#include <AK/String.h>

class CheckBox final : public Widget {
public:
    explicit CheckBox(Widget* parent);
    virtual ~CheckBox() override;

    String caption() const { return m_caption; }
    void setCaption(String&&);

    bool isChecked() const { return m_isChecked; }
    void setIsChecked(bool);

private:
    virtual void paintEvent(PaintEvent&) override;
    virtual void mouseDownEvent(MouseEvent&) override;

    virtual const char* className() const override { return "CheckBox"; }

    String m_caption;
    bool m_isChecked { false };
};

