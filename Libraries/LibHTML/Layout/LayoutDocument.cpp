#include <LibHTML/Layout/LayoutDocument.h>

LayoutDocument::LayoutDocument(const Document& document, StyleProperties&& style_properties)
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
}
