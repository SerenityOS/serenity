#pragma once

#include <LibGUI/GWidget.h>

class GLabel;
class GResizeCorner;

class GStatusBar : public GWidget {
    C_OBJECT(GStatusBar)
public:
    virtual ~GStatusBar() override;

    String text() const;
    void set_text(const StringView&);

protected:
    explicit GStatusBar(GWidget* parent);
    virtual void paint_event(GPaintEvent&) override;

private:
    ObjectPtr<GLabel> m_label;
    ObjectPtr<GResizeCorner> m_corner;
};
