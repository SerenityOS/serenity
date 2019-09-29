#include <LibHTML/DOM/HTMLElement.h>

HTMLElement::HTMLElement(Document& document, const String& tag_name)
    : Element(document, tag_name)
{
}

HTMLElement::~HTMLElement()
{
}
