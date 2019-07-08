#include <LibHTML/Layout/LayoutDocument.h>

LayoutDocument::LayoutDocument(const Document& document, const StyledNode& styled_node)
    : LayoutBlock(&document, &styled_node)
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
