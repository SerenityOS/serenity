#include <LibHTML/Layout/LayoutDocument.h>

LayoutDocument::LayoutDocument(const Document& document)
    : LayoutNode(&document)
{
}

LayoutDocument::~LayoutDocument()
{
}
