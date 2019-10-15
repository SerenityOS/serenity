#pragma once

#include <LibHTML/Layout/LayoutNode.h>

class LayoutBox : public LayoutNodeWithStyleAndBoxModelMetrics {
public:
    const Rect& rect() const { return m_rect; }
    Rect& rect() { return m_rect; }
    void set_rect(const Rect& rect) { m_rect = rect; }

    int x() const { return rect().x(); }
    int y() const { return rect().y(); }
    int width() const { return rect().width(); }
    int height() const { return rect().height(); }
    Size size() const { return rect().size(); }
    Point position() const { return rect().location(); }

    virtual HitTestResult hit_test(const Point& position) const override;
    virtual void set_needs_display() override;

protected:
    LayoutBox(const Node* node, NonnullRefPtr<StyleProperties> style)
        : LayoutNodeWithStyleAndBoxModelMetrics(node, move(style))
    {
    }

    virtual void render(RenderingContext&) override;

private:
    virtual bool is_box() const override { return true; }

    Rect m_rect;
};

template<>
inline bool is<LayoutBox>(const LayoutNode& node)
{
    return node.is_box();
}
