#pragma once

#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/Layout/LineBox.h>

class Element;

class LayoutBlock : public LayoutNode {
public:
    LayoutBlock(const Node*, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutBlock() override;

    virtual const char* class_name() const override { return "LayoutBlock"; }

    virtual void layout() override;
    virtual void render(RenderingContext&) override;

    virtual LayoutNode& inline_wrapper() override;

    bool children_are_inline() const;

    Vector<LineBox>& line_boxes() { return m_line_boxes; }
    const Vector<LineBox>& line_boxes() const { return m_line_boxes; }

    virtual HitTestResult hit_test(const Point&) const override;

private:
    virtual bool is_block() const override { return true; }

    NonnullRefPtr<StyleProperties> style_for_anonymous_block() const;

    void layout_inline_children();
    void layout_block_children();

    void compute_width();
    void compute_position();
    void compute_height();

    Vector<LineBox> m_line_boxes;
};
