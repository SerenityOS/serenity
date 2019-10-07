#include <LibHTML/Frame.h>
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

    int lowest_bottom = 0;
    for_each_child([&](auto& child) {
        if (child.rect().bottom() > lowest_bottom)
            lowest_bottom = child.rect().bottom();
    });
    rect().set_bottom(lowest_bottom);
}
