#pragma once

#include "GWidget.h"

class GListBox final : public GWidget {
public:
    explicit GListBox(GWidget* parent);
    virtual ~GListBox() override;

    void add_item(String&&);
    int selected_index() const { return m_selected_index; }

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual const char* class_name() const override { return "GListBox"; }

    Rect item_rect(int index) const;

    int m_scroll_offset { 0 };
    int m_selected_index { -1 };

    Vector<String> m_items;
};

