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

    virtual const char* class_name() const override { return "LayoutText"; }
    virtual bool is_text() const final { return true; }

    void render_fragment(RenderingContext&, const LineBoxFragment&) const;

    virtual void split_into_lines(LayoutBlock& container) override;

    const StyleProperties& style() const { return parent()->style(); }

private:
    template<typename Callback>
    void for_each_word(Callback) const;
    template<typename Callback>
    void for_each_source_line(Callback) const;
};
