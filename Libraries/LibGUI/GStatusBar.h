#pragma once

#include <LibGUI/GWidget.h>

class GLabel;
class GResizeCorner;

class GStatusBar : public GWidget {
public:
    explicit GStatusBar(GWidget* parent);
    virtual ~GStatusBar() override;

    String text() const;
    void set_text(const StringView&);

    virtual const char* class_name() const override { return "GStatusBar"; }

private:
    virtual void paint_event(GPaintEvent&) override;

    GLabel* m_label { nullptr };
    GResizeCorner* m_corner { nullptr };
};
