#pragma once

#include <LibGUI/GAbstractButton.h>

class GRadioButton : public GAbstractButton {
public:
    GRadioButton(const StringView& text, GWidget* parent);
    virtual ~GRadioButton() override;

    virtual const char* class_name() const override { return "GRadioButton"; }

    virtual void click() override;

protected:
    virtual void paint_event(GPaintEvent&) override;

private:
    // These don't make sense for a radio button, so hide them.
    using GAbstractButton::auto_repeat_interval;
    using GAbstractButton::set_auto_repeat_interval;

    virtual bool is_radio_button() const final { return true; }

    template<typename Callback>
    void for_each_in_group(Callback);
    static Size circle_size();
};

template<>
inline bool is<GRadioButton>(const CObject& object)
{
    if (!is<GWidget>(object))
        return false;
    return to<GWidget>(object).is_radio_button();
}
