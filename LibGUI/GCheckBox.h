#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <LibGUI/GAbstractButton.h>

class GCheckBox : public GAbstractButton {
public:
    explicit GCheckBox(GWidget* parent);
    virtual ~GCheckBox() override;

    virtual void click() override;

    virtual const char* class_name() const override { return "GCheckBox"; }

private:
    virtual void paint_event(GPaintEvent&) override;
};

