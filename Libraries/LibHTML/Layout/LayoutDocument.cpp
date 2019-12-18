#include <LibHTML/Dump.h>
#include <LibHTML/Frame.h>
#include <LibHTML/Layout/LayoutDocument.h>
#include <LibHTML/Layout/LayoutImage.h>

LayoutDocument::LayoutDocument(const Document& document, NonnullRefPtr<StyleProperties> style)
    : LayoutBlock(&document, move(style))
{
}

LayoutDocument::~LayoutDocument()
{
}

void LayoutDocument::layout()
{
    ASSERT(document().frame());
    rect().set_width(document().frame()->size().width());

    LayoutNode::layout();

    ASSERT(!children_are_inline());

    int lowest_bottom = 0;
    for_each_child([&](auto& child) {
        ASSERT(is<LayoutBlock>(child));
        auto& child_block = to<LayoutBlock>(child);
        if (child_block.rect().bottom() > lowest_bottom)
            lowest_bottom = child_block.rect().bottom();
    });
    rect().set_bottom(lowest_bottom);
}

void LayoutDocument::did_set_viewport_rect(Badge<Frame>, const Rect& a_viewport_rect)
{
    FloatRect viewport_rect(a_viewport_rect.x(), a_viewport_rect.y(), a_viewport_rect.width(), a_viewport_rect.height());
    for_each_in_subtree([&](auto& layout_node) {
        if (is<LayoutImage>(layout_node)) {
            auto& image = to<LayoutImage>(layout_node);
            const_cast<HTMLImageElement&>(image.node()).set_volatile({}, !viewport_rect.intersects(image.rect()));
        }
        return IterationDecision::Continue;
    });
}
