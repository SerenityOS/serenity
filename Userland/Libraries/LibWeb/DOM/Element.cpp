/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AnyOf.h>
#include <AK/StringBuilder.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/ResolvedCSSStyleDeclaration.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/CSS/StyleInvalidator.h>
#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/DOM/DOMTokenList.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOMParsing/InnerHTML.h>
#include <LibWeb/Geometry/DOMRect.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/InlineNode.h>
#include <LibWeb/Layout/ListItemBox.h>
#include <LibWeb/Layout/TableBox.h>
#include <LibWeb/Layout/TableCellBox.h>
#include <LibWeb/Layout/TableRowBox.h>
#include <LibWeb/Layout/TableRowGroupBox.h>
#include <LibWeb/Layout/TreeBuilder.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Page/BrowsingContext.h>

namespace Web::DOM {

Element::Element(Document& document, QualifiedName qualified_name)
    : ParentNode(document, NodeType::ELEMENT_NODE)
    , m_qualified_name(move(qualified_name))
    , m_attributes(NamedNodeMap::create(*this))
{
    make_html_uppercased_qualified_name();
}

Element::~Element()
{
}

// https://dom.spec.whatwg.org/#dom-element-getattribute
String Element::get_attribute(const FlyString& name) const
{
    // 1. Let attr be the result of getting an attribute given qualifiedName and this.
    auto const* attribute = m_attributes->get_attribute(name);

    // 2. If attr is null, return null.
    if (!attribute)
        return {};

    // 3. Return attr’s value.
    return attribute->value();
}

// https://dom.spec.whatwg.org/#dom-element-setattribute
ExceptionOr<void> Element::set_attribute(const FlyString& name, const String& value)
{
    // 1. If qualifiedName does not match the Name production in XML, then throw an "InvalidCharacterError" DOMException.
    // FIXME: Proper name validation
    if (name.is_empty())
        return InvalidCharacterError::create("Attribute name must not be empty");

    CSS::StyleInvalidator style_invalidator(document());

    // 2. If this is in the HTML namespace and its node document is an HTML document, then set qualifiedName to qualifiedName in ASCII lowercase.
    // 3. Let attribute be the first attribute in this’s attribute list whose qualified name is qualifiedName, and null otherwise.
    auto* attribute = m_attributes->get_attribute(name);

    // 4. If attribute is null, create an attribute whose local name is qualifiedName, value is value, and node document is this’s node document, then append this attribute to this, and then return.
    if (!attribute) {
        auto new_attribute = Attribute::create(document(), name, value);
        m_attributes->append_attribute(new_attribute);
    }

    // 5. Change attribute to value.
    else {
        attribute->set_value(value);
    }

    parse_attribute(name, value);
    return {};
}

// https://dom.spec.whatwg.org/#dom-element-removeattribute
void Element::remove_attribute(const FlyString& name)
{
    CSS::StyleInvalidator style_invalidator(document());
    m_attributes->remove_attribute(name);
}

// https://dom.spec.whatwg.org/#dom-element-hasattribute
bool Element::has_attribute(const FlyString& name) const
{
    return m_attributes->get_attribute(name) != nullptr;
}

bool Element::has_class(const FlyString& class_name, CaseSensitivity case_sensitivity) const
{
    return any_of(m_classes, [&](auto& it) {
        return case_sensitivity == CaseSensitivity::CaseSensitive
            ? it == class_name
            : it.to_lowercase() == class_name.to_lowercase();
    });
}

RefPtr<Layout::Node> Element::create_layout_node()
{
    auto style = document().style_computer().compute_style(*this);
    const_cast<Element&>(*this).m_specified_css_values = style;
    auto display = style->display();

    if (display.is_none())
        return nullptr;

    if (local_name() == "noscript" && document().is_scripting_enabled())
        return nullptr;

    if (display.is_table_inside())
        return adopt_ref(*new Layout::TableBox(document(), this, move(style)));

    if (display.is_list_item())
        return adopt_ref(*new Layout::ListItemBox(document(), *this, move(style)));

    if (display.is_table_row())
        return adopt_ref(*new Layout::TableRowBox(document(), this, move(style)));

    if (display.is_table_cell())
        return adopt_ref(*new Layout::TableCellBox(document(), this, move(style)));

    if (display.is_table_row_group() || display.is_table_header_group() || display.is_table_footer_group())
        return adopt_ref(*new Layout::TableRowGroupBox(document(), *this, move(style)));

    if (display.is_table_column() || display.is_table_column_group() || display.is_table_caption()) {
        // FIXME: This is just an incorrect placeholder until we improve table layout support.
        return adopt_ref(*new Layout::BlockContainer(document(), this, move(style)));
    }

    if (display.is_inline_outside()) {
        if (display.is_flow_root_inside()) {
            auto block = adopt_ref(*new Layout::BlockContainer(document(), this, move(style)));
            block->set_inline(true);
            return block;
        }
        if (display.is_flow_inside())
            return adopt_ref(*new Layout::InlineNode(document(), *this, move(style)));

        TODO();
    }

    if (display.is_flow_inside() || display.is_flow_root_inside() || display.is_flex_inside())
        return adopt_ref(*new Layout::BlockContainer(document(), this, move(style)));

    TODO();
}

void Element::parse_attribute(const FlyString& name, const String& value)
{
    if (name == HTML::AttributeNames::class_) {
        auto new_classes = value.split_view(' ');
        m_classes.clear();
        m_classes.ensure_capacity(new_classes.size());
        for (auto& new_class : new_classes) {
            m_classes.unchecked_append(new_class);
        }
        if (m_class_list)
            m_class_list->associated_attribute_changed(value);
    } else if (name == HTML::AttributeNames::style) {
        auto parsed_style = parse_css_declaration(CSS::ParsingContext(document()), value);
        if (!parsed_style.is_null()) {
            m_inline_style = CSS::ElementInlineCSSStyleDeclaration::create_and_take_properties_from(*this, parsed_style.release_nonnull());
            set_needs_style_update(true);
        }
    }
}

enum class StyleDifference {
    None,
    NeedsRepaint,
    NeedsRelayout,
};

static StyleDifference compute_style_difference(CSS::StyleProperties const& old_style, CSS::StyleProperties const& new_style, Layout::NodeWithStyle const& node)
{
    if (old_style == new_style)
        return StyleDifference::None;

    bool needs_repaint = false;
    bool needs_relayout = false;

    if (new_style.display() != old_style.display())
        needs_relayout = true;

    if (new_style.color_or_fallback(CSS::PropertyID::Color, node, Color::Black) != old_style.color_or_fallback(CSS::PropertyID::Color, node, Color::Black))
        needs_repaint = true;
    else if (new_style.color_or_fallback(CSS::PropertyID::BackgroundColor, node, Color::Black) != old_style.color_or_fallback(CSS::PropertyID::BackgroundColor, node, Color::Black))
        needs_repaint = true;

    if (needs_relayout)
        return StyleDifference::NeedsRelayout;
    if (needs_repaint)
        return StyleDifference::NeedsRepaint;
    return StyleDifference::None;
}

void Element::recompute_style()
{
    set_needs_style_update(false);
    VERIFY(parent());
    auto old_specified_css_values = m_specified_css_values;
    auto new_specified_css_values = document().style_computer().compute_style(*this);
    m_specified_css_values = new_specified_css_values;
    if (!layout_node()) {
        if (new_specified_css_values->display().is_none())
            return;
        // We need a new layout tree here!
        Layout::TreeBuilder tree_builder;
        tree_builder.build(*this);
        return;
    }

    auto diff = StyleDifference::NeedsRelayout;
    if (old_specified_css_values && layout_node())
        diff = compute_style_difference(*old_specified_css_values, *new_specified_css_values, *layout_node());
    if (diff == StyleDifference::None)
        return;
    layout_node()->apply_style(*new_specified_css_values);
    if (diff == StyleDifference::NeedsRelayout) {
        document().set_needs_layout();
        return;
    }
    if (diff == StyleDifference::NeedsRepaint) {
        layout_node()->set_needs_display();
    }
}

NonnullRefPtr<CSS::StyleProperties> Element::computed_style()
{
    auto element_computed_style = CSS::ResolvedCSSStyleDeclaration::create(*this);
    auto properties = CSS::StyleProperties::create();

    for (auto i = to_underlying(CSS::first_property_id); i <= to_underlying(CSS::last_property_id); ++i) {
        auto property_id = (CSS::PropertyID)i;
        auto maybe_value = element_computed_style->property(property_id);
        if (!maybe_value.has_value())
            continue;
        properties->set_property(property_id, maybe_value.release_value().value);
    }

    return properties;
}

RefPtr<DOMTokenList> const& Element::class_list()
{
    if (!m_class_list)
        m_class_list = DOMTokenList::create(*this, HTML::AttributeNames::class_);
    return m_class_list;
}

// https://dom.spec.whatwg.org/#dom-element-matches
DOM::ExceptionOr<bool> Element::matches(StringView selectors) const
{
    auto maybe_selectors = parse_selector(CSS::ParsingContext(static_cast<ParentNode&>(const_cast<Element&>(*this))), selectors);
    if (!maybe_selectors.has_value())
        return DOM::SyntaxError::create("Failed to parse selector");

    auto sel = maybe_selectors.value();
    for (auto& s : sel) {
        if (SelectorEngine::matches(s, *this))
            return true;
    }
    return false;
}

ExceptionOr<void> Element::set_inner_html(String const& markup)
{
    auto result = DOMParsing::inner_html_setter(*this, markup);
    if (result.is_exception())
        return result.exception();

    set_needs_style_update(true);
    return {};
}

// https://w3c.github.io/DOM-Parsing/#dom-innerhtml-innerhtml
String Element::inner_html() const
{
    return serialize_fragment(/* FIXME: Providing true for the require well-formed flag (which may throw) */);
}

bool Element::is_focused() const
{
    return document().focused_element() == this;
}

bool Element::is_active() const
{
    return document().active_element() == this;
}

NonnullRefPtr<HTMLCollection> Element::get_elements_by_class_name(FlyString const& class_name)
{
    return HTMLCollection::create(*this, [class_name, quirks_mode = document().in_quirks_mode()](Element const& element) {
        return element.has_class(class_name, quirks_mode ? CaseSensitivity::CaseInsensitive : CaseSensitivity::CaseSensitive);
    });
}

void Element::set_shadow_root(RefPtr<ShadowRoot> shadow_root)
{
    if (m_shadow_root == shadow_root)
        return;
    m_shadow_root = move(shadow_root);
    invalidate_style();
}

NonnullRefPtr<CSS::CSSStyleDeclaration> Element::style_for_bindings()
{
    if (!m_inline_style)
        m_inline_style = CSS::ElementInlineCSSStyleDeclaration::create(*this);
    return *m_inline_style;
}

// https://dom.spec.whatwg.org/#element-html-uppercased-qualified-name
void Element::make_html_uppercased_qualified_name()
{
    // This is allowed by the spec: "User agents could optimize qualified name and HTML-uppercased qualified name by storing them in internal slots."
    if (namespace_() == Namespace::HTML /* FIXME: and its node document is an HTML document */)
        m_html_uppercased_qualified_name = qualified_name().to_uppercase();
    else
        m_html_uppercased_qualified_name = qualified_name();
}

// https://html.spec.whatwg.org/multipage/webappapis.html#queue-an-element-task
void Element::queue_an_element_task(HTML::Task::Source source, Function<void()> steps)
{
    auto task = HTML::Task::create(source, &document(), [strong_this = NonnullRefPtr(*this), steps = move(steps)] {
        steps();
    });
    HTML::main_thread_event_loop().task_queue().add(move(task));
}

// https://html.spec.whatwg.org/multipage/syntax.html#void-elements
bool Element::is_void_element() const
{
    return local_name().is_one_of(HTML::TagNames::area, HTML::TagNames::base, HTML::TagNames::br, HTML::TagNames::col, HTML::TagNames::embed, HTML::TagNames::hr, HTML::TagNames::img, HTML::TagNames::input, HTML::TagNames::link, HTML::TagNames::meta, HTML::TagNames::param, HTML::TagNames::source, HTML::TagNames::track, HTML::TagNames::wbr);
}

// https://html.spec.whatwg.org/multipage/parsing.html#serializes-as-void
bool Element::serializes_as_void() const
{
    return is_void_element() || local_name().is_one_of(HTML::TagNames::basefont, HTML::TagNames::bgsound, HTML::TagNames::frame, HTML::TagNames::keygen);
}

// https://drafts.csswg.org/cssom-view/#dom-element-getboundingclientrect
NonnullRefPtr<Geometry::DOMRect> Element::get_bounding_client_rect() const
{
    // FIXME: Support inline layout nodes as well.

    if (!layout_node() || !layout_node()->is_box())
        return Geometry::DOMRect::create(0, 0, 0, 0);

    VERIFY(document().browsing_context());
    auto viewport_offset = document().browsing_context()->viewport_scroll_offset();

    auto& box = static_cast<Layout::Box const&>(*layout_node());
    return Geometry::DOMRect::create(box.absolute_rect().translated(-viewport_offset.x(), -viewport_offset.y()));
}

int Element::client_top() const
{
    if (!layout_node() || !layout_node()->is_box())
        return 0;
    auto& box = static_cast<Layout::Box const&>(*layout_node());
    return box.absolute_rect().top();
}

int Element::client_left() const
{
    if (!layout_node() || !layout_node()->is_box())
        return 0;
    auto& box = static_cast<Layout::Box const&>(*layout_node());
    return box.absolute_rect().left();
}

int Element::client_width() const
{
    if (!layout_node() || !layout_node()->is_box())
        return 0;
    auto& box = static_cast<Layout::Box const&>(*layout_node());
    return box.absolute_rect().width();
}

int Element::client_height() const
{
    if (!layout_node() || !layout_node()->is_box())
        return 0;
    auto& box = static_cast<Layout::Box const&>(*layout_node());
    return box.absolute_rect().height();
}

void Element::children_changed()
{
    Node::children_changed();
    set_needs_style_update(true);
}

}
