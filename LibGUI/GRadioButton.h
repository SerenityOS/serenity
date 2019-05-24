#pragma once

#include <LibGUI/GAbstractButton.h>

class GRadioButton : public GAbstractButton {
public:
    GRadioButton(const String& text, GWidget* parent);
    virtual ~GRadioButton() override;

    virtual const char* class_name() const override { return "GRadioButton"; }

    virtual void click() override;

protected:
    virtual void paint_event(GPaintEvent&) override;

private:
    virtual bool is_radio_button() const final { return true; }

    template<typename Callback> void for_each_in_group(Callback);
    static Size circle_size();
};
