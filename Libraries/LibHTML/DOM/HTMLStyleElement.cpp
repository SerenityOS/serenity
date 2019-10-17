#include <AK/StringBuilder.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLStyleElement.h>
#include <LibHTML/DOM/Text.h>
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
    StringBuilder builder;
    for_each_child([&](auto& child) {
        if (is<Text>(child))
            builder.append(to<Text>(child).text_content());
    });
    m_stylesheet = parse_css(builder.to_string());
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
