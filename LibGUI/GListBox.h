#pragma once

#include "GWidget.h"

class GListBox final : public GWidget {
public:
    explicit GListBox(GWidget* parent);
    virtual ~GListBox() override;

    void addItem(String&&);
    int selectedIndex() const { return m_selectedIndex; }

private:
    virtual void paintEvent(GPaintEvent&) override;
    virtual void mouseDownEvent(GMouseEvent&) override;
    virtual const char* class_name() const override { return "GListBox"; }

    Rect item_rect(int index) const;

    int m_scrollOffset { 0 };
    int m_selectedIndex { -1 };

    Vector<String> m_items;
};

