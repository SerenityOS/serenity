#pragma once

#include <LibHTML/Layout/LayoutReplaced.h>

class GWidget;

class LayoutWidget : public LayoutReplaced {
public:
    LayoutWidget(const Element&, GWidget&);
    virtual ~LayoutWidget() override;

    virtual void layout() override;
    virtual void render(RenderingContext&) override;

    GWidget& widget() { return m_widget; }
    const GWidget& widget() const { return m_widget; }

    virtual bool is_widget() const final { return true; }

private:
    virtual const char* class_name() const override { return "LayoutWidget"; }

    NonnullRefPtr<GWidget> m_widget;
};

template<>
inline bool is<LayoutWidget>(const LayoutNode& node)
{
    return node.is_widget();
}
