#pragma once

#include <LibHTML/DOM/Text.h>
#include <LibHTML/Layout/LayoutNode.h>

class LineBoxFragment;

class LayoutText : public LayoutNode {
public:
    explicit LayoutText(const Text&);
    virtual ~LayoutText() override;

    const Text& node() const { return static_cast<const Text&>(*LayoutNode::node()); }

    const String& text_for_style(const StyleProperties&) const;
    const String& text_for_rendering() const { return m_text_for_rendering; }

    virtual const char* class_name() const override { return "LayoutText"; }
    virtual bool is_text() const final { return true; }

    void render_fragment(RenderingContext&, const LineBoxFragment&) const;

    virtual void split_into_lines(LayoutBlock& container) override;

    const StyleProperties& style() const { return parent()->style(); }

private:
    void split_preformatted_into_lines(LayoutBlock& container);

    template<typename Callback>
    void for_each_word(Callback) const;

    String m_text_for_rendering;
};

template<>
inline bool is<LayoutText>(const LayoutNode& node)
{
    return node.is_text();
}
