#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutDocument.h>
#include <stdio.h>

Document::Document()
    : ParentNode(NodeType::DOCUMENT_NODE)
{
}

Document::~Document()
{
}

StyleResolver& Document::style_resolver()
{
    if (!m_style_resolver)
        m_style_resolver = make<StyleResolver>(*this);
    return *m_style_resolver;
}

void Document::normalize()
{
    if (first_child() != nullptr && first_child()->is_element()) {
        const Element& el = static_cast<const Element&>(*first_child());
        if (el.tag_name() == "html")
            return;
    }

    NonnullRefPtr<Element> body = adopt(*new Element("body"));
    NonnullRefPtr<Element> html = adopt(*new Element("html"));
    html->append_child(body);
    this->donate_all_children_to(body);
    this->append_child(html);
}
