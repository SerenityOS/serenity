#pragma once

#include <LibDraw/FloatRect.h>
#include <LibHTML/Layout/LayoutNode.h>

class LayoutBox : public LayoutNodeWithStyleAndBoxModelMetrics {
public:
    const FloatRect& rect() const { return m_rect; }
    FloatRect& rect() { return m_rect; }
    void set_rect(const FloatRect& rect) { m_rect = rect; }

    float x() const { return rect().x(); }
    float y() const { return rect().y(); }
    float width() const { return rect().width(); }
    float height() const { return rect().height(); }
    FloatSize size() const { return rect().size(); }
    FloatPoint position() const { return rect().location(); }

    virtual HitTestResult hit_test(const Point& position) const override;
    virtual void set_needs_display() override;

    bool is_body() const;

protected:
    LayoutBox(const Node* node, NonnullRefPtr<StyleProperties> style)
        : LayoutNodeWithStyleAndBoxModelMetrics(node, move(style))
    {
    }

    virtual void render(RenderingContext&) override;

private:
    virtual bool is_box() const override { return true; }

    FloatRect m_rect;
};

template<>
inline bool is<LayoutBox>(const LayoutNode& node)
{
    return node.is_box();
}
