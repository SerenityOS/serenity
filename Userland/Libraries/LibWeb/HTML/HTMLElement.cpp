/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/BreakNode.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/UIEvents/EventNames.h>

namespace Web::HTML {

HTMLElement::HTMLElement(DOM::Document& document, QualifiedName qualified_name)
    : Element(document, move(qualified_name))
    , m_dataset(DOMStringMap::create(*this))
{
}

HTMLElement::~HTMLElement()
{
}

HTMLElement::ContentEditableState HTMLElement::content_editable_state() const
{
    auto contenteditable = attribute(HTML::AttributeNames::contenteditable);
    // "true", an empty string or a missing value map to the "true" state.
    if ((!contenteditable.is_null() && contenteditable.is_empty()) || contenteditable.equals_ignoring_case("true"))
        return ContentEditableState::True;
    // "false" maps to the "false" state.
    if (contenteditable.equals_ignoring_case("false"))
        return ContentEditableState::False;
    // Having no such attribute or an invalid value maps to the "inherit" state.
    return ContentEditableState::Inherit;
}

bool HTMLElement::is_editable() const
{
    switch (content_editable_state()) {
    case ContentEditableState::True:
        return true;
    case ContentEditableState::False:
        return false;
    case ContentEditableState::Inherit:
        return parent() && parent()->is_editable();
    default:
        VERIFY_NOT_REACHED();
    }
}

String HTMLElement::content_editable() const
{
    switch (content_editable_state()) {
    case ContentEditableState::True:
        return "true";
    case ContentEditableState::False:
        return "false";
    case ContentEditableState::Inherit:
        return "inherit";
    default:
        VERIFY_NOT_REACHED();
    }
}

// https://html.spec.whatwg.org/multipage/interaction.html#contenteditable
DOM::ExceptionOr<void> HTMLElement::set_content_editable(const String& content_editable)
{
    if (content_editable.equals_ignoring_case("inherit")) {
        remove_attribute(HTML::AttributeNames::contenteditable);
        return {};
    }
    if (content_editable.equals_ignoring_case("true")) {
        set_attribute(HTML::AttributeNames::contenteditable, "true");
        return {};
    }
    if (content_editable.equals_ignoring_case("false")) {
        set_attribute(HTML::AttributeNames::contenteditable, "false");
        return {};
    }
    return DOM::SyntaxError::create("Invalid contentEditable value, must be 'true', 'false', or 'inherit'");
}

void HTMLElement::set_inner_text(StringView text)
{
    remove_all_children();
    append_child(document().create_text_node(text));

    set_needs_style_update(true);
}

String HTMLElement::inner_text()
{
    StringBuilder builder;

    // innerText for element being rendered takes visibility into account, so force a layout and then walk the layout tree.
    document().update_layout();
    if (!layout_node())
        return text_content();

    Function<void(const Layout::Node&)> recurse = [&](auto& node) {
        for (auto* child = node.first_child(); child; child = child->next_sibling()) {
            if (is<Layout::TextNode>(child))
                builder.append(verify_cast<Layout::TextNode>(*child).text_for_rendering());
            if (is<Layout::BreakNode>(child))
                builder.append('\n');
            recurse(*child);
        }
    };
    recurse(*layout_node());

    return builder.to_string();
}

// // https://drafts.csswg.org/cssom-view/#dom-htmlelement-offsettop
int HTMLElement::offset_top() const
{
    if (is<HTML::HTMLBodyElement>(this) || !layout_node() || !parent_element() || !parent_element()->layout_node())
        return 0;
    auto position = layout_node()->box_type_agnostic_position();
    auto parent_position = parent_element()->layout_node()->box_type_agnostic_position();
    return position.y() - parent_position.y();
}

// https://drafts.csswg.org/cssom-view/#dom-htmlelement-offsetleft
int HTMLElement::offset_left() const
{
    if (is<HTML::HTMLBodyElement>(this) || !layout_node() || !parent_element() || !parent_element()->layout_node())
        return 0;
    auto position = layout_node()->box_type_agnostic_position();
    auto parent_position = parent_element()->layout_node()->box_type_agnostic_position();
    return position.x() - parent_position.x();
}

// https://drafts.csswg.org/cssom-view/#dom-htmlelement-offsetwidth
int HTMLElement::offset_width() const
{
    if (!layout_node() || !layout_node()->is_box())
        return 0;
    return static_cast<Layout::Box const&>(*layout_node()).border_box_width();
}

// https://drafts.csswg.org/cssom-view/#dom-htmlelement-offsetheight
int HTMLElement::offset_height() const
{
    if (!layout_node() || !layout_node()->is_box())
        return 0;
    return static_cast<Layout::Box const&>(*layout_node()).border_box_height();
}

bool HTMLElement::cannot_navigate() const
{
    // FIXME: Return true if element's node document is not fully active
    return !is<HTML::HTMLAnchorElement>(this) && !is_connected();
}

void HTMLElement::parse_attribute(const FlyString& name, const String& value)
{
    Element::parse_attribute(name, value);

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                          \
    if (name == HTML::AttributeNames::attribute_name) {                  \
        set_event_handler_attribute(event_name, EventHandler { value }); \
    }
    ENUMERATE_GLOBAL_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE
}

}
