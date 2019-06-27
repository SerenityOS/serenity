#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/CSS/StyleSheet.h>

StyleResolver::StyleResolver(Document& document)
    : m_document(document)
{
}

StyleResolver::~StyleResolver()
{
}

OwnPtr<LayoutStyle> StyleResolver::resolve_document_style(const Document& document)
{
    UNUSED_PARAM(document);
    return nullptr;
}

OwnPtr<LayoutStyle> StyleResolver::resolve_element_style(const Element& element)
{
    UNUSED_PARAM(element);
    return nullptr;
}
