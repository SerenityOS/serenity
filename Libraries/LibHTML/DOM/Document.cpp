#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOM/HTMLBodyElement.h>
#include <LibHTML/DOM/HTMLHeadElement.h>
#include <LibHTML/DOM/HTMLHtmlElement.h>
#include <LibHTML/DOM/HTMLTitleElement.h>
#include <LibHTML/Frame.h>
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

const HTMLBodyElement* Document::body() const
{
    auto* html = document_element();
    if (!html)
        return nullptr;
    return static_cast<const HTMLBodyElement*>(html->first_child_with_tag_name("body"));
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

void Document::attach_to_frame(Badge<Frame>, Frame& frame)
{
    m_frame = frame.make_weak_ptr();
}

void Document::detach_from_frame(Badge<Frame>, Frame&)
{
}

Color Document::background_color() const
{
    auto* body_element = body();
    if (!body_element)
        return Color::White;

    auto* body_layout_node = body_element->layout_node();
    if (!body_layout_node)
        return Color::White;

    auto background_color = body_layout_node->style().property("background-color");
    if (!background_color.has_value() || !background_color.value()->is_color())
        return Color::White;

    return background_color.value()->to_color();
}

URL Document::complete_url(const String& string) const
{
    URL url(string);
    if (url.is_valid())
        return url;

    FileSystemPath fspath(m_url.path());
    StringBuilder builder;
    builder.append('/');
    for (int i = 0; i < fspath.parts().size(); ++i) {
        if (i == fspath.parts().size() - 1)
            break;
        builder.append(fspath.parts()[i]);
        builder.append('/');
    }
    builder.append(string);
    auto built = builder.to_string();
    fspath = FileSystemPath(built);

    url = m_url;
    url.set_path(fspath.string());
    return url;
}
