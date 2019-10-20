#include <LibGUI/GPainter.h>
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
    if (!is<LayoutText>(layout_node()))
        return false;
    auto& layout_text = to<LayoutText>(layout_node());
    auto text = layout_text.node().data().substring_view(m_start, m_length);
    return text == " ";
}
