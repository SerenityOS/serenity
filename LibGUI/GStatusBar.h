#pragma once

#include <LibGUI/GWidget.h>

class GLabel;

class GStatusBar : public GWidget {
public:
    explicit GStatusBar(GWidget* parent);
    virtual ~GStatusBar() override;

    String text() const;
    void set_text(String&&);

private:
    virtual const char* class_name() const override { return "GStatusBar"; }
    virtual void paint_event(GPaintEvent&) override;

    GLabel* m_label { nullptr };
};
