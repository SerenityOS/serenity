#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOM/HTMLHeadElement.h>
#include <LibHTML/DOM/HTMLHtmlElement.h>
#include <LibHTML/DOM/HTMLTitleElement.h>
#include <LibHTML/Layout/LayoutDocument.h>
#include <stdio.h>

Document::Document()
    : ParentNode(*this, NodeType::DOCUMENT_NODE)
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

    NonnullRefPtr<Element> body = adopt(*new Element(*this, "body"));
    NonnullRefPtr<Element> html = adopt(*new Element(*this, "html"));
    html->append_child(body);
    this->donate_all_children_to(body);
    this->append_child(html);
}

const HTMLHtmlElement* Document::document_element() const
{
    return static_cast<const HTMLHtmlElement*>(first_child_with_tag_name("html"));
}

const HTMLHeadElement* Document::head() const
{
    auto* html = document_element();
    if (!html)
        return nullptr;
    return static_cast<const HTMLHeadElement*>(html->first_child_with_tag_name("head"));
}

String Document::title() const
{
    auto* head_element = head();
    if (!head_element)
        return {};

    auto* title_element = static_cast<const HTMLTitleElement*>(head_element->first_child_with_tag_name("title"));
    if (!title_element)
        return {};

    return title_element->text_content();
}
