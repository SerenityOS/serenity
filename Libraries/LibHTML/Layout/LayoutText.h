#pragma once

#include <LibHTML/DOM/Text.h>
#include <LibHTML/Layout/LayoutInline.h>

class Font;
class LineBoxFragment;

class LayoutText : public LayoutInline {
public:
    explicit LayoutText(const Text&);
    virtual ~LayoutText() override;

    const Text& node() const { return static_cast<const Text&>(*LayoutNode::node()); }

    const String& text_for_style(const StyleProperties&) const;

    virtual const char* class_name() const override { return "LayoutText"; }
    virtual bool is_text() const final { return true; }

    void render_fragment(RenderingContext&, const LineBoxFragment&) const;

    virtual void split_into_lines(LayoutBlock& container) override;

    const StyleProperties& style_properties() const { return parent()->style_properties(); }

private:
    template<typename Callback>
    void for_each_word(Callback) const;
    template<typename Callback>
    void for_each_source_line(Callback) const;

    void load_font();

    RefPtr<Font> m_font;
};
