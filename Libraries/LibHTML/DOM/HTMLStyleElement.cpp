#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLStyleElement.h>
#include <LibHTML/Parser/CSSParser.h>

HTMLStyleElement::HTMLStyleElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLStyleElement::~HTMLStyleElement()
{
}

void HTMLStyleElement::inserted_into(Node& new_parent)
{
    m_stylesheet = parse_css(text_content());
    if (m_stylesheet)
        document().add_sheet(*m_stylesheet);
    HTMLElement::inserted_into(new_parent);
}

void HTMLStyleElement::removed_from(Node& old_parent)
{
    if (m_stylesheet) {
        // FIXME: Remove the sheet from the document
    }
    return HTMLElement::removed_from(old_parent);
}
