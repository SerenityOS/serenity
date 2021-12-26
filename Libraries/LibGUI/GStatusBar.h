#pragma once

#include <LibGUI/GWidget.h>

class GLabel;
class GResizeCorner;

class GStatusBar : public GWidget {
    C_OBJECT(GStatusBar)
public:
    explicit GStatusBar(GWidget* parent);
    virtual ~GStatusBar() override;

    String text() const;
    void set_text(const StringView&);

private:
    virtual void paint_event(GPaintEvent&) override;

    ObjectPtr<GLabel> m_label;
    GResizeCorner* m_corner { nullptr };
};
