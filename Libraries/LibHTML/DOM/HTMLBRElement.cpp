#include <LibHTML/DOM/HTMLBRElement.h>
#include <LibHTML/Layout/LayoutBreak.h>

HTMLBRElement::HTMLBRElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLBRElement::~HTMLBRElement()
{
}

RefPtr<LayoutNode> HTMLBRElement::create_layout_node(const StyleResolver&, const StyleProperties*) const
{
    return adopt(*new LayoutBreak(*this));
}
