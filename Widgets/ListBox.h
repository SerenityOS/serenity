#pragma once

#include "Widget.h"

class ListBox final : public Widget {
public:
    explicit ListBox(Widget* parent);
    virtual ~ListBox() override;

    void addItem(String&&);
    int selectedIndex() const { return m_selectedIndex; }

private:
    virtual void paintEvent(PaintEvent&) override;
    virtual void mouseDownEvent(MouseEvent&) override;
    virtual const char* class_name() const override { return "ListBox"; }

    Rect item_rect(int index) const;

    int m_scrollOffset { 0 };
    int m_selectedIndex { -1 };

    Vector<String> m_items;
};

