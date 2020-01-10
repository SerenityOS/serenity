#pragma once

#include <AK/Function.h>
#include <AK/String.h>
#include <LibGUI/GAbstractButton.h>

class GCheckBox : public GAbstractButton {
    C_OBJECT(GCheckBox)
public:
    virtual ~GCheckBox() override;

    virtual void click() override;

private:
    GCheckBox(const StringView&, GWidget* parent);
    explicit GCheckBox(GWidget* parent);

    // These don't make sense for a check box, so hide them.
    using GAbstractButton::auto_repeat_interval;
    using GAbstractButton::set_auto_repeat_interval;

    virtual void paint_event(GPaintEvent&) override;
};
