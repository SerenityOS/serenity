#pragma once

#include "GWidget.h"
#include <AK/AKString.h>

class GCheckBox final : public GWidget {
public:
    explicit GCheckBox(GWidget* parent);
    virtual ~GCheckBox() override;

    String caption() const { return m_caption; }
    void setCaption(String&&);

    bool isChecked() const { return m_isChecked; }
    void setIsChecked(bool);

private:
    virtual void paintEvent(GPaintEvent&) override;
    virtual void mouseDownEvent(GMouseEvent&) override;

    virtual const char* class_name() const override { return "GCheckBox"; }

    String m_caption;
    bool m_isChecked { false };
};

