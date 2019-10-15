#include <LibHTML/Frame.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Layout/LayoutDocument.h>

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
