/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/StringBuilder.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/Layout/BreakNode.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/UIEvents/EventNames.h>

namespace Web::HTML {

HTMLElement::HTMLElement(DOM::Document& document, const QualifiedName& qualified_name)
    : Element(document, qualified_name)
{
}

HTMLElement::~HTMLElement()
{
}

HTMLElement::ContentEditableState HTMLElement::content_editable_state() const
{
    auto contenteditable = attribute(HTML::AttributeNames::contenteditable);
    // "true" and the empty string map to the "true" state.
    if ((!contenteditable.is_null() && contenteditable.is_empty()) || contenteditable.equals_ignoring_case("true"))
        return ContentEditableState::True;
    // "false" maps to the "false" state.
    if (contenteditable.equals_ignoring_case("false"))
        return ContentEditableState::False;
    // "inherit", an invalid value, and a missing value all map to the "inherit" state.
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
        ASSERT_NOT_REACHED();
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
        ASSERT_NOT_REACHED();
    }
}

void HTMLElement::set_content_editable(const String& content_editable)
{
    if (content_editable.equals_ignoring_case("inherit")) {
        remove_attribute(HTML::AttributeNames::contenteditable);
        return;
    }
    if (content_editable.equals_ignoring_case("true")) {
        set_attribute(HTML::AttributeNames::contenteditable, "true");
        return;
    }
    if (content_editable.equals_ignoring_case("false")) {
        set_attribute(HTML::AttributeNames::contenteditable, "false");
        return;
    }
    // FIXME: otherwise the attribute setter must throw a "SyntaxError" DOMException.
}

void HTMLElement::set_inner_text(StringView text)
{
    remove_all_children();
    append_child(document().create_text_node(text));

    set_needs_style_update(true);
    document().invalidate_layout();
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
                builder.append(downcast<Layout::TextNode>(*child).text_for_rendering());
            if (is<Layout::BreakNode>(child))
                builder.append('\n');
            recurse(*child);
        }
    };
    recurse(*layout_node());

    return builder.to_string();
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
