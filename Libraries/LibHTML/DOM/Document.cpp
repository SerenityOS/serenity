#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/CTimer.h>
#include <LibGUI/GApplication.h>
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
#include <LibHTML/Layout/LayoutTreeBuilder.h>
#include <stdio.h>

Document::Document()
    : ParentNode(*this, NodeType::DOCUMENT_NODE)
    , m_style_resolver(make<StyleResolver>(*this))
{
    m_style_update_timer = CTimer::construct();
    m_style_update_timer->set_single_shot(true);
    m_style_update_timer->set_interval(0);
    m_style_update_timer->on_timeout = [this] {
        update_style();
    };
}

Document::~Document()
{
}

void Document::schedule_style_update()
{
    if (m_style_update_timer->is_active())
        return;
    m_style_update_timer->start();
}

bool Document::is_child_allowed(const Node& node) const
{
    switch (node.type()) {
    case NodeType::DOCUMENT_NODE:
    case NodeType::TEXT_NODE:
        return false;
    case NodeType::COMMENT_NODE:
        return true;
    case NodeType::DOCUMENT_TYPE_NODE:
        return !first_child_of_type<DocumentType>();
    case NodeType::ELEMENT_NODE:
        return !first_child_of_type<Element>();
    default:
        return false;
    }
}

void Document::fixup()
{
    if (!first_child() || !is<DocumentType>(*first_child()))
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
    layout();
}

void Document::detach_from_frame(Badge<Frame>, Frame&)
{
    m_layout_root = nullptr;
    m_frame = nullptr;
}

Color Document::background_color(const Palette& palette) const
{
    auto default_color = palette.base();
    auto* body_element = body();
    if (!body_element)
        return default_color;

    auto* body_layout_node = body_element->layout_node();
    if (!body_layout_node)
        return default_color;

    auto background_color = body_layout_node->style().property(CSS::PropertyID::BackgroundColor);
    if (!background_color.has_value() || !background_color.value()->is_color())
        return default_color;

    return background_color.value()->to_color(*this);
}

RefPtr<GraphicsBitmap> Document::background_image() const
{
    auto* body_element = body();
    if (!body_element)
        return {};

    auto* body_layout_node = body_element->layout_node();
    if (!body_layout_node)
        return {};

    auto background_image = body_layout_node->style().property(CSS::PropertyID::BackgroundImage);
    if (!background_image.has_value() || !background_image.value()->is_image())
        return {};

    auto& image_value = static_cast<const ImageStyleValue&>(*background_image.value());
    if (!image_value.bitmap())
        return {};

    return *image_value.bitmap();
}

URL Document::complete_url(const String& string) const
{
    return m_url.complete_url(string);
}

void Document::force_layout()
{
    m_layout_root = nullptr;
    layout();
}

void Document::layout()
{
    if (!m_layout_root) {
        LayoutTreeBuilder tree_builder;
        m_layout_root = tree_builder.build(*this);
    }
    m_layout_root->layout();
    m_layout_root->set_needs_display();
}

void Document::update_style()
{
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (element.needs_style_update())
            element.recompute_style();
        return IterationDecision::Continue;
    });
    update_layout();
}

void Document::update_layout()
{
    if (!frame())
        return;

    layout();
    if (on_layout_updated)
        on_layout_updated();
}

RefPtr<LayoutNode> Document::create_layout_node(const StyleProperties*) const
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

const LayoutDocument* Document::layout_node() const
{
    return static_cast<const LayoutDocument*>(Node::layout_node());
}

LayoutDocument* Document::layout_node()
{
    return static_cast<LayoutDocument*>(Node::layout_node());
}

void Document::set_inspected_node(Node* node)
{
    if (m_inspected_node == node)
        return;

    if (m_inspected_node && m_inspected_node->layout_node())
        m_inspected_node->layout_node()->set_needs_display();

    m_inspected_node = node;

    if (m_inspected_node && m_inspected_node->layout_node())
        m_inspected_node->layout_node()->set_needs_display();
}

void Document::set_hovered_node(Node* node)
{
    if (m_hovered_node == node)
        return;

    RefPtr<Node> old_hovered_node = move(m_hovered_node);
    m_hovered_node = node;

    invalidate_style();
}

const Element* Document::get_element_by_id(const String& id) const
{
    const Element* found_element = nullptr;
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (element.attribute("id") == id) {
            found_element = &element;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return found_element;
}

Vector<const Element*> Document::get_elements_by_name(const String& name) const
{
    Vector<const Element*> elements;
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (element.attribute("name") == name)
            elements.append(&element);
        return IterationDecision::Continue;
    });
    return elements;
}
