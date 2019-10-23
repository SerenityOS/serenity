#pragma once

#include <LibGUI/GWidget.h>

class GLabel;
class GResizeCorner;

class GStatusBar : public GWidget {
    C_OBJECT(GStatusBar)
public:
    virtual ~GStatusBar() override;

    String text() const;
    String text(int index) const;
    void set_text(const StringView&);
    void set_text(int index, const StringView&);

protected:
    explicit GStatusBar(GWidget* parent);
    explicit GStatusBar(int label_count, GWidget* parent);
    virtual void paint_event(GPaintEvent&) override;

private:
    NonnullRefPtr<GLabel> create_label();
    NonnullRefPtrVector<GLabel> m_labels;
    RefPtr<GResizeCorner> m_corner;
};
