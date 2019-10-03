#pragma once

#include <LibHTML/DOM/Text.h>
#include <LibHTML/Layout/LayoutInline.h>

class Font;
class LineBoxFragment;

class LayoutText : public LayoutInline {
public:
    LayoutText(const Text&, StyleProperties&&);
    virtual ~LayoutText() override;

    const Text& node() const { return static_cast<const Text&>(*LayoutNode::node()); }

    const String& text() const;

    virtual const char* class_name() const override { return "LayoutText"; }
    virtual bool is_text() const final { return true; }

    void render_fragment(RenderingContext&, const LineBoxFragment&) const;

    virtual void split_into_lines(LayoutBlock& container) override;

private:
    template<typename Callback>
    void for_each_word(Callback) const;
    template<typename Callback>
    void for_each_source_line(Callback) const;

    void load_font();
    void compute_runs();

    RefPtr<Font> m_font;
};
