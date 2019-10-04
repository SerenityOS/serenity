#include <LibHTML/Layout/LayoutDocument.h>

LayoutDocument::LayoutDocument(const Document& document, NonnullRefPtr<StyleProperties> style_properties)
    : LayoutBlock(&document, move(style_properties))
{
}

LayoutDocument::~LayoutDocument()
{
}

void LayoutDocument::layout()
{
    rect().set_width(style().size().width());

    LayoutNode::layout();

    int lowest_bottom = 0;
    for_each_child([&](auto& child) {
        if (child.rect().bottom() > lowest_bottom)
            lowest_bottom = child.rect().bottom();
    });
    rect().set_bottom(lowest_bottom);
}
