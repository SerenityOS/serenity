#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <LibGUI/GAbstractButton.h>

class GCheckBox : public GAbstractButton {
public:
    GCheckBox(const StringView&, GWidget* parent);
    explicit GCheckBox(GWidget* parent);
    virtual ~GCheckBox() override;

    virtual void click() override;

    virtual const char* class_name() const override { return "GCheckBox"; }

private:
    // These don't make sense for a check box, so hide them.
    using GAbstractButton::auto_repeat_interval;
    using GAbstractButton::set_auto_repeat_interval;

    virtual void paint_event(GPaintEvent&) override;
};
