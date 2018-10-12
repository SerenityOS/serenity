#pragma once

#include "Widget.h"

class ListBox final : public Widget {
public:
    explicit ListBox(Widget* parent);
    virtual ~ListBox() override;

    void addItem(String&&);
    int selectedIndex() const { return m_selectedIndex; }

private:
    virtual void onPaint(PaintEvent&) override;
    virtual void onMouseDown(MouseEvent&) override;

    unsigned itemHeight() const;

    unsigned m_scrollOffset { 0 };
    int m_selectedIndex { -1 };

    Vector<String> m_items;
};

