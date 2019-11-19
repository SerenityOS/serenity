#include <LibGUI/GPainter.h>
#include <LibHTML/Layout/LayoutDocument.h>
#include <LibHTML/Layout/LayoutText.h>
#include <LibHTML/Layout/LineBoxFragment.h>
#include <LibHTML/RenderingContext.h>

void LineBoxFragment::render(RenderingContext& context)
{
    for (auto* ancestor = layout_node().parent(); ancestor; ancestor = ancestor->parent()) {
        if (!ancestor->is_visible())
            return;
    }

    if (is<LayoutText>(layout_node())) {
        to<LayoutText>(layout_node()).render_fragment(context, *this);
    }
}

bool LineBoxFragment::is_justifiable_whitespace() const
{
    return text() == " ";
}

StringView LineBoxFragment::text() const
{
    if (!is<LayoutText>(layout_node()))
        return {};
    return to<LayoutText>(layout_node()).text_for_rendering().substring_view(m_start, m_length);
}

int LineBoxFragment::text_index_at(float x) const
{
    if (!layout_node().is_text())
        return 0;
    auto& layout_text = to<LayoutText>(layout_node());
    auto& font = layout_text.style().font();
    Utf8View view(text());

    float relative_x = x - m_rect.location().x();
    float glyph_spacing = font.glyph_spacing();

    float width_so_far = 0;
    for (auto it = view.begin(); it != view.end(); ++it) {
        float glyph_width = font.glyph_or_emoji_width(*it);
        if ((width_so_far + glyph_width + glyph_spacing) > relative_x)
            return m_start + view.byte_offset_of(it);
        width_so_far += glyph_width + glyph_spacing;
    }
    return m_start + m_length - 1;
}
