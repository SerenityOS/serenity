#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/DocumentType.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOM/ElementFactory.h>
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

void Document::fixup()
{
    if (!is<DocumentType>(first_child()))
        prepend_child(adopt(*new DocumentType(*this)));

    if (is<HTMLHtmlElement>(first_child()->next_sibling()))
        return;

    auto body = create_element(*this, "body");
    auto html = create_element(*this, "html");
    html->append_child(body);
    this->donate_all_children_to(body);
    this->append_child(html);
}

const HTMLHtmlElement* Document::document_element() const
{
    return first_child_of_type<HTMLHtmlElement>();
}

const HTMLHeadElement* Document::head() const
{
    auto* html = document_element();
    if (!html)
        return nullptr;
    return html->first_child_of_type<HTMLHeadElement>();
}

const HTMLBodyElement* Document::body() const
{
    auto* html = document_element();
    if (!html)
        return nullptr;
    return html->first_child_of_type<HTMLBodyElement>();
}

String Document::title() const
{
    auto* head_element = head();
    if (!head_element)
        return {};

    auto* title_element = head_element->first_child_of_type<HTMLTitleElement>();
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
    m_frame = nullptr;
}

Color Document::background_color() const
{
    auto* body_element = body();
    if (!body_element)
        return Color::White;

    auto* body_layout_node = body_element->layout_node();
    if (!body_layout_node)
        return Color::White;

    auto background_color = body_layout_node->style().property(CSS::PropertyID::BackgroundColor);
    if (!background_color.has_value() || !background_color.value()->is_color())
        return Color::White;

    return background_color.value()->to_color(*this);
}

URL Document::complete_url(const String& string) const
{
    URL url(string);
    if (url.is_valid())
        return url;

    FileSystemPath fspath(m_url.path());
    StringBuilder builder;
    builder.append('/');

    bool document_url_ends_in_slash = m_url.path()[m_url.path().length() - 1] == '/';

    for (int i = 0; i < fspath.parts().size(); ++i) {
        if (i == fspath.parts().size() - 1 && !document_url_ends_in_slash)
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

void Document::invalidate_layout()
{
    if (on_invalidate_layout)
        on_invalidate_layout();
}

RefPtr<LayoutNode> Document::create_layout_node(const StyleResolver&, const StyleProperties*) const
{
    return adopt(*new LayoutDocument(*this, StyleProperties::create()));
}

void Document::set_link_color(Color color)
{
    m_link_color = color;
}

void Document::set_active_link_color(Color color)
{
    m_active_link_color = color;
}

void Document::set_visited_link_color(Color color)
{
    m_visited_link_color = color;
}
